/**
 * Copyright (C) 2012 the original author or authors.
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

package conf;


import controllers.AppController;
import controllers.AuthController;
import controllers.UserController;
import ninja.AssetsController;
import ninja.Router;
import ninja.application.ApplicationRoutes;

public class Routes implements ApplicationRoutes {

    @Override
    public void init(Router router) {

        router.OPTIONS().route("/.*").with(AppController.class, "cors");
        router.GET().route("/assets/{fileName: .*}").with(AssetsController.class, "serveStatic");
        router.GET().route("/").with(AppController.class, "index");

        router.POST().route("/auth/signup").with(AuthController.class, "signup");
        router.POST().route("/auth/login").with(AuthController.class, "login");
        router.POST().route("/auth/logout").with(AuthController.class, "logout");

        router.GET().route("/auth/check").with(AuthController.class, "check");

        router.METHOD("PATCH").route("/me").with(UserController.class, "update");
        router.GET().route("/me/feeds").with(UserController.class, "getAllFeed");
        router.PUT().route("/me/feeds").with(UserController.class, "addFeed");
        router.DELETE().route("/me/feeds/{id}").with(UserController.class, "deleteFeed");
        router.GET().route("/me/feeds/{id}").with(UserController.class, "getOneFeed");

        router.GET().route("/me/categories").with(UserController.class, "getAllCategory");
        router.POST().route("/me/categories").with(UserController.class, "createCategory");
        router.PUT().route("/me/categories/{cat_id}/feed/{feed_id}").with(UserController.class, "addFeedToCategory");
    }

}
