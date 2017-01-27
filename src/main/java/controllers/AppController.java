package controllers;

import filters.CORSFilter;
import ninja.FilterWith;
import ninja.Result;
import ninja.Results;
import ninja.jpa.UnitOfWork;

import javax.inject.Singleton;

@Singleton
@FilterWith(CORSFilter.class)
public class AppController {
    @UnitOfWork
    public Result cors() {
        return Results.json();
    }

    public Result index() {
        return Results.html();
    }
}
