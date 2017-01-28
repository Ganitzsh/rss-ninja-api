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
import filters.AddCORS;
import filters.AuthCheck;
import filters.CORSFilter;
import filters.CTCheck;
import models.User;
import models.User_;
import ninja.*;
import ninja.cache.NinjaCache;
import ninja.jpa.UnitOfWork;
import ninja.params.Param;
import ninja.session.Session;
import ninja.validation.FieldViolation;
import ninja.validation.JSR303Validation;
import ninja.validation.Validation;
import tools.SessionIdentifierGenerator;
import tools.TokenAuthority;

import javax.inject.Inject;
import javax.inject.Provider;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Iterator;


@Singleton
@FilterWith(AddCORS.class)
public class AuthController {

    @Inject
    Provider<EntityManager> entitiyManagerProvider;
    @Inject
    NinjaCache ninjaCache;

    @UnitOfWork
    @FilterWith(CTCheck.class)
    public Result signup(Context context, Session session, @JSR303Validation User u, Validation validation) {
        if (validation.hasBeanViolations()) {
            ArrayList map = new ArrayList();
            for (Iterator<FieldViolation> i = validation.getBeanViolations().listIterator(); i.hasNext();) {
                FieldViolation elem = i.next();
                map.add(elem.field + " " + elem.constraintViolation.getMessageKey());
            }
            return Results.json().status(400).render(new JSendResp(400, map));
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
            return Results.json().render(new RespAuth(u.getId(), token, u.getEmail()));
        } catch (Exception e) {
            return Results.json().status(400).render(new JSendResp(400, e));
        }
    }

    @UnitOfWork
    @FilterWith(CTCheck.class)
    public Result login(Context context, Session session, User req) {
        EntityManager entitymanager = entitiyManagerProvider.get();
        CriteriaBuilder cb = entitymanager.getCriteriaBuilder();

        try {
            CriteriaQuery<User> query = cb.createQuery(User.class);
            Root<User> a = query.from(User.class);
            query.where(
                    cb.equal(a.get(User_.email), req.getEmail()),
                    cb.equal(a.get(User_.password), req.getPassword())
            );
            User u = entitymanager.createQuery(query).getSingleResult();
            String token = SessionIdentifierGenerator.nextSessionId();
            context.addCookie(Cookie.builder("token", token).build());
            session.put("token", token);
            session.put("email", u.getEmail());
            session.put("id", String.valueOf(u.getId()));
            ninjaCache.set(token, u.getId());
            return Results.json().render(new RespAuth(u.getId(), token, u.getEmail()));
        } catch (Exception e) {
            return Results.json().status(400).render(new JSendResp(400, e));
        }
    }


    @UnitOfWork
    public Result logout(Context context, Session session) {
        Cookie c = context.getCookie("token");
        if (c != null) {
            ninjaCache.delete(c.getValue());
        }
        session.clear();
        return Results.text().status(200);
    }

    @UnitOfWork
    @FilterWith(AuthCheck.class)
    public Result check(Context context, Session session) {
        String token = context.getCookieValue("token");
        Long id = (Long) ninjaCache.get(token);
        return Results.json().render(new RespAuth(id, token, session.get("email")));
    }

    public class RespAuth {
        public Long id;
        public String token;
        public String email;

        public RespAuth(Long id, String token, String email) {
            this.id = id;
            this.token = token;
            this.email = email;
        }

        public RespAuth(Long id, String token) {
            this.id = id;
            this.token = token;
        }
    }
}