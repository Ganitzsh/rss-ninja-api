package controllers;

import com.google.inject.Singleton;
import com.rometools.rome.feed.synd.*;
import com.rometools.rome.io.SyndFeedInput;
import filters.CORSFilter;
import ninja.FilterWith;
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
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Singleton
@FilterWith(CORSFilter.class)
public class UserController {
    @Inject
    Provider<EntityManager> entitiyManagerProvider;
    @Inject
    NinjaCache ninjaCache;


    @UnitOfWork
    public Result update(Context context, Session session, User updatedUser) {
        if (!TokenAuthority.isValid(context.getCookieValue("token"), ninjaCache)) {
            return Results.noContent().status(401).render(context.getCookieValue("token"));
        }
        EntityManager em = entitiyManagerProvider.get();
        Long id = (Long) ninjaCache.get(session.get("token"));

        User currentUser = em.find(User.class, id);
        if (currentUser == null) {
            return Results.status(400);
        }

        em.getTransaction().begin();
        currentUser.setEmail(updatedUser.getEmail() == null ? currentUser.getEmail() : updatedUser.getEmail());
        currentUser.setFirstName(updatedUser.getFirstName() == null ? currentUser.getFirstName() : updatedUser.getFirstName());
        currentUser.setLastName(updatedUser.getLastName() == null ? currentUser.getLastName() : updatedUser.getLastName());
        currentUser.setUsername(updatedUser.getUsername() == null ? currentUser.getUsername() : updatedUser.getUsername());
        currentUser.setPassword(updatedUser.getPassword() == null ? currentUser.getPassword() : updatedUser.getPassword());
        em.getTransaction().commit();
        return Results.json().render(currentUser);
    }

    @UnitOfWork
    public Result addFeed(Context context, Session session, RSSFeed feed) {
        if (!TokenAuthority.isValid(context.getCookieValue("token"), ninjaCache)) {
            return Results.noContent().status(401);
        }
        EntityManager em = entitiyManagerProvider.get();
        Long id = (Long) ninjaCache.get(session.get("token"));

        feed.setOwner_id(id);
        em.getTransaction().begin();
        em.persist(feed);
        em.getTransaction().commit();
        return Results.json().render(feed);
    }

    @UnitOfWork
    public Result getAllFeed(Context context, Session session) {
        if (!TokenAuthority.isValid(context.getCookieValue("token"), ninjaCache)) {
            return Results.noContent().status(401);
        }
        EntityManager em = entitiyManagerProvider.get();
        Long id = (Long) ninjaCache.get(session.get("token"));

        em.getTransaction().begin();
        User u = entitiyManagerProvider.get().find(User.class, id);
        em.getTransaction().commit();
        return Results.json().render(u.getFeeds());
    }

    @UnitOfWork
    public Result getOneFeed(Context context, Session session, @PathParam("id") Long fId) {
        if (!TokenAuthority.isValid(context.getCookieValue("token"), ninjaCache)) {
            return Results.noContent().status(401);
        }
        EntityManager em = entitiyManagerProvider.get();
        Long id = (Long) ninjaCache.get(session.get("token"));

        RSSFeed selection = em.find(RSSFeed.class, fId);
        if (selection == null) {
            return Results.status(400);
        }
        try {
            StringBuilder result = new StringBuilder();
            URL url = new URL(selection.getUrl());
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            if (conn.getResponseCode() != 200) {
                return Results.noContent().status(conn.getResponseCode());
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

            List<SyndEntry> entries = f.getEntries();
            Iterator<SyndEntry> it = entries.iterator();
            Feed feed = new Feed();
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
            return Results.json().render(feed);
        } catch (Exception e) {
            System.out.println(e);
            return Results.json().status(400).render(e);
        }
    }

    @UnitOfWork
    public Result deleteFeed(Context context, Session session, @PathParam("id") Long fId) {
        if (!TokenAuthority.isValid(context.getCookieValue("token"), ninjaCache)) {
            return Results.noContent().status(401);
        }
        EntityManager em = entitiyManagerProvider.get();
        Long id = (Long) ninjaCache.get(session.get("token"));

        RSSFeed feed = em.find(RSSFeed.class, fId);
        if (feed == null) {
            return Results.noContent().status(400);
        }
        em.getTransaction().begin();
        em.remove(feed);
        em.getTransaction().commit();
        return Results.noContent().status(200);
    }

}
