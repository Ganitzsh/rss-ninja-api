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


import models.User;
import org.junit.Test;

import ninja.NinjaDocTester;
import org.doctester.testbrowser.Request;
import org.doctester.testbrowser.Response;
import static org.junit.Assert.assertEquals;

public class ApiControllerDocTesterTest extends NinjaDocTester {

    String URL_SIGNUP = "/auth/signup";
    String URL_LOGIN = "/auth/login";
    String URL_LOGOUT = "/auth/logout";
    String URL_CHECK = "/auth/check";

    @Test
    public void testLogin() {

        User u = new User();
        u.setEmail("test@email.com");
        u.setPassword("12345");
        Request reqFailed = Request
                .POST()
                .contentTypeApplicationJson()
                .payload(u)
                .url(testServerUrl().path(URL_LOGIN));

        Response response = makeRequest(reqFailed);

        assertEquals(400, response.httpStatus);
    }

    @Test
    public void testLogout() {

        Request req = Request
                .POST()
                .url(testServerUrl().path(URL_LOGOUT));

        Response response = makeRequest(req);

        assertEquals(200, response.httpStatus);
    }

    @Test
    public void testAuthCheck() {

        Request req = Request
                .GET()
                .url(testServerUrl().path(URL_CHECK));

        Response response = makeRequest(req);

        assertEquals(401, response.httpStatus);
    }

}
