package io.github.thomashtn.valorant.tracker.shared.web;

public final class ApiRoutes {
    /**
     * Executes the api routes operation.
     */
    private ApiRoutes() {
    }
    public static final String API = "/api";
    public static final String PLAYERS = API+"/players";
    public static final String CHALLENGES = API+"/challenges";
    public static final String RANKINGS = API+"/rankings";
    public static final String ADMIN = API+"/admin";
    public static final String SYNCHRONIZATIONS = ADMIN+"/synchronizations";
}
