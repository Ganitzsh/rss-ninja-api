package controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Singleton;
import com.rometools.rome.feed.synd.*;
import com.rometools.rome.io.SyndFeedInput;
import filters.AddCORS;
import filters.AuthCheck;
import filters.CORSFilter;
import filters.CTCheck;
import models.Category;
import models.ItemJSON;
import ninja.FilterWith;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import rss.Author;
import rss.Content;
import rss.Feed;
import rss.Item;
import models.RSSFeed;
import models.User;
import ninja.Context;
import ninja.Result;
import ninja.Results;
import ninja.cache.NinjaCache;
import ninja.jpa.UnitOfWork;
import ninja.params.PathParam;
import ninja.session.Session;
import org.xml.sax.InputSource;
import tools.TokenAuthority;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.persistence.EntityManager;
import java.io.*;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

@Singleton
@FilterWith({
        AuthCheck.class,
        AddCORS.class
})
public class UserController {
    @Inject
    Provider<EntityManager> entitiyManagerProvider;
    @Inject
    NinjaCache ninjaCache;

    @UnitOfWork
    @FilterWith(CTCheck.class)
    public Result update(Session session, User updatedUser) {
        EntityManager em = entitiyManagerProvider.get();
        Long id = (Long) ninjaCache.get(session.get("token"));

        try {
            User currentUser = em.find(User.class, id);

            em.getTransaction().begin();
            currentUser.setEmail(updatedUser.getEmail() == null ? currentUser.getEmail() : updatedUser.getEmail());
            currentUser.setFirstName(updatedUser.getFirstName() == null ? currentUser.getFirstName() : updatedUser.getFirstName());
            currentUser.setLastName(updatedUser.getLastName() == null ? currentUser.getLastName() : updatedUser.getLastName());
            currentUser.setUsername(updatedUser.getUsername() == null ? currentUser.getUsername() : updatedUser.getUsername());
            currentUser.setPassword(updatedUser.getPassword() == null ? currentUser.getPassword() : updatedUser.getPassword());
            em.getTransaction().commit();
            return Results.json().render(currentUser);
        } catch (Exception e) {
            return Results.json().status(400).render(new JSendResp(400, e));
        }
    }

    @UnitOfWork
    @FilterWith(CTCheck.class)
    public Result addFeed(Session session, RSSFeed feed) {
        EntityManager em = entitiyManagerProvider.get();
        Long id = (Long) ninjaCache.get(session.get("token"));

        try {
            feed.setOwner_id(id);
            em.getTransaction().begin();
            em.persist(feed);
            em.getTransaction().commit();
            return Results.json().render(feed);
        } catch (Exception e) {
            return Results.json().status(400).render(new JSendResp(400, e));
        }
    }

    @UnitOfWork
    public Result getAllFeed(Session session) {
        EntityManager em = entitiyManagerProvider.get();
        Long id = (Long) ninjaCache.get(session.get("token"));

        try {
            em.getTransaction().begin();
            User u = entitiyManagerProvider.get().find(User.class, id);
            em.getTransaction().commit();
            return Results.json().render(u.getFeeds());
        } catch (Exception e) {
            return Results.json().status(400).render(new JSendResp(400, e));
        }
    }

    @UnitOfWork
    public Result getOneFeed(Session session, @PathParam("id") Long fId) {
        EntityManager em = entitiyManagerProvider.get();
        String feedURL = null;

        try {
            RSSFeed selection = em.find(RSSFeed.class, fId);
            Feed feed;

            feedURL = selection.getUrl();
            String state = (String) ninjaCache.get("feed:" + feedURL + ":state");
            if (state != null) {
                if (state.equals("in_progress")) {
                    return Results.text().status(429);
                }
                if ((feed = (Feed) ninjaCache.get("feed:" + feedURL)) != null) {
                    long seconds = (new Date().getTime()-feed.getCachedDate().getTime())/1000;
                    System.out.println("Time till last caching: " + seconds);
                    if (seconds < 60) {
                        return Results.json().render(feed);
                    }
                }
            }
            ninjaCache.set("feed:" + feedURL + ":state", "in_progress");
            feed = new Feed();
            StringBuilder result = new StringBuilder();
            URL url = new URL(feedURL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "RSS Agg 0.2");
            conn.setRequestMethod("GET");
            if (conn.getResponseCode() != 200) {
                return Results.text().status(conn.getResponseCode());
            }
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }
            rd.close();
            String rawXML = result.toString();
            InputSource source = new InputSource(new StringReader(rawXML));
            SyndFeedInput input = new SyndFeedInput();
            SyndFeed f = input.build(source);

            feed.setUri(f.getUri());
            feed.setCopyright(f.getCopyright());
            feed.setDocs(f.getDocs());
            feed.setPublished(f.getPublishedDate());
            feed.setGenerator(f.getGenerator());
            feed.setEncoding(f.getEncoding());
            feed.setTitle(f.getTitle());
            feed.setType(f.getFeedType());
            List<SyndCategory> feedCategories = f.getCategories();
            Iterator feedCategoriesIt = feedCategories.listIterator();
            while (feedCategoriesIt.hasNext()) {
                SyndCategory category = (SyndCategory) feedCategoriesIt.next();
                feed.getCategories().add(category.getName());
            }
            List<SyndPerson> feedAuthors = f.getAuthors();
            Iterator feedAuthorsIt = feedAuthors.listIterator();
            while (feedAuthorsIt.hasNext()) {
                SyndPerson author = (SyndPerson) feedAuthorsIt.next();
                Author a = new Author();
                a.setEmail(author.getEmail());
                a.setUri(author.getUri());
                a.setName(author.getName());
                feed.getAuthors().add(a);
            }
            List<SyndLink> feedLinks = f.getLinks();
            Iterator linksIt = feedLinks.listIterator();
            while (linksIt.hasNext()) {
                SyndLink link = (SyndLink) linksIt.next();
                feed.getLinks().add(link.getHref());
            }
            List<SyndEntry> entries = f.getEntries();
            Iterator<SyndEntry> it = entries.iterator();
            List<Item> items = new ArrayList<>();
            while (it.hasNext()) {
                SyndEntry entry = it.next();

                Item item = new Item();
                if (entry.getDescription() != null) {
                    item.setDescription(entry.getDescription().getValue());
                }
                item.setTitle(entry.getTitle());
                item.setAuthor(entry.getAuthor());
                item.setLink(entry.getLink());
                item.setUpdatedDate(entry.getUpdatedDate());
                item.setPublishedDate(entry.getPublishedDate());
                item.setUri(entry.getUri());

                List<SyndCategory> categories = entry.getCategories();
                Iterator categoriesIt = categories.listIterator();
                while (categoriesIt.hasNext()) {
                    SyndCategory category = (SyndCategory) categoriesIt.next();
                    item.getCategories().add(category.getName());
                }
                List<SyndPerson> authors = entry.getAuthors();
                Iterator authorsIt = authors.listIterator();
                while (authorsIt.hasNext()) {
                    SyndPerson author = (SyndPerson) authorsIt.next();
                    Author a = new Author();
                    a.setEmail(author.getEmail());
                    a.setUri(author.getUri());
                    a.setName(author.getName());
                    item.getAuthors().add(a);
                }
                List<SyndLink> entryLinks = entry.getLinks();
                Iterator entryLinksIt = entryLinks.listIterator();
                while (entryLinksIt.hasNext()) {
                    SyndLink link = (SyndLink) entryLinksIt.next();
                    item.getLinks().add(link.getHref());
                }
                List<SyndContent> contents = entry.getContents();
                Iterator i = contents.listIterator();
                while (i.hasNext()) {
                    SyndContent c = (SyndContent) i.next();
                    Content content = new Content();
                    content.setType(c.getType());
                    content.setValue(c.getValue());
                    item.getContents().add(content);
                }
                feed.getItems().add(item);
            }
            feed.setCachedDate(new Date());
            ninjaCache.set("feed:" + feedURL, feed);
            ninjaCache.set("feed:" + feedURL + ":state", "done");
            return Results.json().render(feed);
        } catch (Exception e) {
            e.printStackTrace();
            if (feedURL != null) {
                ninjaCache.set("feed:" + feedURL + ":state", "error");
                ninjaCache.set("feed:" + feedURL + ":exception", e);
            }
            return Results.json().status(400).render(new JSendResp(400, e));
        }
    }

    @UnitOfWork
    public Result deleteFeed(Session session, @PathParam("id") Long fId) {
        EntityManager em = entitiyManagerProvider.get();

        try {
            RSSFeed feed = em.find(RSSFeed.class, fId);
            em.getTransaction().begin();
            em.remove(feed);
            em.getTransaction().commit();
            return this.getAllFeed(session);
        } catch (Exception e) {
            return Results.json().status(400).render(new JSendResp(400, e));
        }
    }

    @UnitOfWork
    @FilterWith(CTCheck.class)
    public Result createCategory(Session session, Category category) {
        EntityManager em = entitiyManagerProvider.get();
        Long id = (Long) ninjaCache.get(session.get("token"));

        try {
            category.setOwner_id(id);
            em.getTransaction().begin();
            em.persist(category);
            em.getTransaction().commit();
            return Results.json().render(category);
        } catch (Exception e) {
            return Results.json().status(400).render(new JSendResp(400, e));
        }
    }

    @UnitOfWork
    public Result addFeedToCategory(Session session, @PathParam("cat_id") Long cId, @PathParam("feed_id") Long fId) {
        EntityManager em = entitiyManagerProvider.get();

        try {
            Category category = em.find(Category.class, cId);
            RSSFeed feed = em.find(RSSFeed.class, fId);

            if (category.getFeeds() != null) {
                em.getTransaction().begin();
                category.getFeeds().add(feed);
                em.getTransaction().commit();
            }
            return Results.json().render(category);
        } catch (Exception e) {
            return Results.json().status(400).render(new JSendResp(400, e));
        }
    }

    @UnitOfWork
    public Result getAllCategory(Session session) {
        EntityManager em = entitiyManagerProvider.get();
        Long id = (Long) ninjaCache.get(session.get("token"));

        try {
            em.getTransaction().begin();
            User u = entitiyManagerProvider.get().find(User.class, id);
            em.getTransaction().commit();
            return Results.json().render(u.getCategories());
        } catch (Exception e) {
            return Results.json().status(400).render(new JSendResp(400, e));
        }
    }

    @UnitOfWork
    public Result starItem(Session session, Item item) {
        EntityManager em = entitiyManagerProvider.get();
        Long id = (Long) ninjaCache.get(session.get("token"));

        try {
            ItemJSON itemJSON = new ItemJSON();
            ObjectMapper mapper = new ObjectMapper();
            String jsonInString = mapper.writeValueAsString(item);
            itemJSON.setContent(jsonInString);
            itemJSON.setLink(item.getLink());
            itemJSON.setOwner_id(id);
            em.getTransaction().begin();
            em.persist(itemJSON);
            em.getTransaction().commit();
            return Results.json().render(item);
        } catch (Exception e) {
            return Results.json().status(400).render(new JSendResp(400, e));
        }
    }

    @UnitOfWork
    public Result getOneStarredItem(Session session, @PathParam("id") Long iid) {
        EntityManager em = entitiyManagerProvider.get();

        try {
            ObjectMapper mapper = new ObjectMapper();
            ItemJSON itemJSON = em.find(ItemJSON.class, iid);
            Item item = mapper.readValue(itemJSON.getContent(), Item.class);
            item.setId(itemJSON.getId().toString());
            return Results.json().render(item);
        } catch (Exception e) {
            return Results.json().status(400).render(new JSendResp(400, e));
        }
    }

    @UnitOfWork
    public Result getAllStarredItem(Session session) {
        EntityManager em = entitiyManagerProvider.get();
        Long id = (Long) ninjaCache.get(session.get("token"));

        try {
            em.getTransaction().begin();
            User u = entitiyManagerProvider.get().find(User.class, id);
            em.getTransaction().commit();
            List<Item> ret = new ArrayList<>();
            List<ItemJSON> itemJSON = u.getStarred();
            Iterator<ItemJSON> it = itemJSON.iterator();
            while (it.hasNext()) {
                ItemJSON i = it.next();
                ObjectMapper mapper = new ObjectMapper();
                Item item = mapper.readValue(i.getContent(), Item.class);
                item.setId(i.getId().toString());
                ret.add(item);
            }
            return Results.json().render(ret);
        } catch (Exception e) {
            return Results.json().status(400).render(new JSendResp(400, e));
        }
    }

    @UnitOfWork
    public Result deleteStaredItem(Session session, @PathParam("id") Long id) {
        EntityManager em = entitiyManagerProvider.get();

        try {
            ItemJSON item = em.find(ItemJSON.class, id);
            em.getTransaction().begin();
            em.remove(item);
            em.getTransaction().commit();
            return this.getAllStarredItem(session);
        } catch (Exception e) {
            return Results.json().status(400).render(new JSendResp(400, e));
        }
    }
}
