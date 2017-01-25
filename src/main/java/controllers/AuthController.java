/**
 * Copyright (C) 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers;

import com.google.inject.Singleton;
import models.User;
import models.User_;
import ninja.Context;
import ninja.Cookie;
import ninja.Result;
import ninja.Results;
import ninja.cache.NinjaCache;
import ninja.exceptions.BadRequestException;
import ninja.jpa.UnitOfWork;
import ninja.params.Param;
import ninja.session.Session;
import ninja.validation.FieldViolation;
import ninja.validation.JSR303Validation;
import ninja.validation.Validation;
import tools.SessionIdentifierGenerator;

import javax.inject.Inject;
import javax.inject.Provider;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Iterator;


@Singleton
public class AuthController {

    @Inject
    Provider<EntityManager> entitiyManagerProvider;
    @Inject
    NinjaCache ninjaCache;

    @UnitOfWork
    public Result signup(Context context, Session session, @JSR303Validation User u, Validation validation) {

        if (validation.hasBeanViolations()) {
            ArrayList map = new ArrayList();
            for (Iterator<FieldViolation> i = validation.getBeanViolations().listIterator(); i.hasNext();) {
                FieldViolation elem = i.next();
                map.add(elem.field + " " + elem.constraintViolation.getMessageKey());
            }
            return Results.json().status(400).render(map);
        }
        try {
            EntityManager entitymanager = entitiyManagerProvider.get();
            entitymanager.getTransaction().begin();
            entitymanager.persist(u);
            entitymanager.getTransaction().commit();
            String token = SessionIdentifierGenerator.nextSessionId();
            context.addCookie(Cookie.builder("token", token).build());
            ninjaCache.set(token, u.getId());
            session.put("token", token);
            session.put("email", u.getEmail());
            session.put("id", String.valueOf(u.getId()));
            return Results.json().render(new RespAuth(u.getId(), token));
        } catch (Exception e) {
            throw new BadRequestException(e);
        }
    }

    @UnitOfWork
    public Result login(Context context, Session session, @Param("email") String email, @Param("password") String password) {
        EntityManager entitymanager = entitiyManagerProvider.get();
        CriteriaBuilder cb = entitymanager.getCriteriaBuilder();

        try {
            CriteriaQuery<User> query = cb.createQuery(User.class);
            Root<User> a = query.from(User.class);
            query.where(
                    cb.equal(a.get(User_.email), email),
                    cb.equal(a.get(User_.password), password)
            );
            User u = entitymanager.createQuery(query).getSingleResult();
            String token = SessionIdentifierGenerator.nextSessionId();
            context.addCookie(Cookie.builder("token", token).build());
            session.put("token", token);
            session.put("email", u.getEmail());
            session.put("id", String.valueOf(u.getId()));
            ninjaCache.set(token, u.getId());
            return Results.json().render(new RespAuth(u.getId(), token));
        } catch (NoResultException e) {
            return Results.json().status(401);
        }
    }

    @UnitOfWork
    public Result logout(Context context, Session session) {
        Cookie c = context.getCookie("token");
        ninjaCache.delete(c.getValue());
        session.clear();
        return Results.status(200);
    }

    @UnitOfWork
    public Result ping(Context context, Session session) {
        return Results.json().render(String.join(",", session.getData().values()));
    }

    public class RespAuth {
        public Long id;
        public String token;

        public RespAuth(Long id, String token) {
            this.id = id;
            this.token = token;
        }
    }
}


