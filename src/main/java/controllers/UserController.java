package controllers;

import com.google.inject.Singleton;
import models.RSSFeed;
import models.User;
import ninja.Context;
import ninja.Result;
import ninja.Results;
import ninja.cache.NinjaCache;
import ninja.jpa.UnitOfWork;
import ninja.params.PathParam;
import ninja.session.Session;
import tools.TokenAuthority;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.persistence.EntityManager;
import java.util.List;
import java.util.Map;

@Singleton
public class UserController {
    @Inject
    Provider<EntityManager> entitiyManagerProvider;
    @Inject
    NinjaCache ninjaCache;


    @UnitOfWork
    public Result update(Context context, Session session, User updatedUser) {
        if (!TokenAuthority.isValid(context.getCookieValue("token"), ninjaCache)) {
            return Results.json().status(401).render(context.getCookieValue("token"));
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
            return Results.json().status(401);
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
            return Results.json().status(401);
        }
        EntityManager em = entitiyManagerProvider.get();
        Long id = (Long) ninjaCache.get(session.get("token"));

        em.getTransaction().begin();
        User u = entitiyManagerProvider.get().find(User.class, id);
        em.getTransaction().commit();
        return Results.json().render(u.getFeeds());
    }

    @UnitOfWork
    public Result deleteFeed(Context context, Session session, @PathParam("id") Long fId) {
        if (!TokenAuthority.isValid(context.getCookieValue("token"), ninjaCache)) {
            return Results.json().status(401);
        }
        EntityManager em = entitiyManagerProvider.get();
        Long id = (Long) ninjaCache.get(session.get("token"));

        RSSFeed feed = em.find(RSSFeed.class, fId);
        if (feed == null) {
            return Results.status(400);
        }
        em.getTransaction().begin();
        em.remove(feed);
        em.getTransaction().commit();
        return Results.status(200);
    }

}
