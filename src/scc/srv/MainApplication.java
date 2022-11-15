package scc.srv;

import java.util.HashSet;
import java.util.Set;

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
