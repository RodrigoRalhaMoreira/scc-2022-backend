package scc.srv;

import scc.srv.resources.BidResource;
import scc.srv.resources.MediaResource;
import scc.srv.resources.UsersResource;
import scc.srv.resources.ControlResource;
import scc.srv.resources.AuctionsResource;
import scc.srv.resources.QuestionsResource;

import java.util.Set;
import java.util.HashSet;
import jakarta.ws.rs.core.Application;

public class MainApplication extends Application {
    private static Set<Object> singletons = new HashSet<Object>();
    private Set<Class<?>> resources = new HashSet<Class<?>>();

    public MainApplication() {
        resources.add(ControlResource.class);
        resources.add(MediaResource.class);
        resources.add(UsersResource.class);
        resources.add(AuctionsResource.class);
        resources.add(QuestionsResource.class);
        resources.add(BidResource.class);

        singletons.add(new MediaResource());
        singletons.add(new UsersResource());
        singletons.add(new AuctionsResource());
        singletons.add(new QuestionsResource());
        singletons.add(new BidResource());
    }

    @Override
    public Set<Class<?>> getClasses() {
        return resources;
    }

    @Override
    public Set<Object> getSingletons() {
        return singletons;
    }

    public static Set<Object> getSingletonsSet() {
        return singletons;
    }
}
