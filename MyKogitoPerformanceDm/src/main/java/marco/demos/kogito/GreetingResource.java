package marco.demos.kogito;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.micrometer.core.instrument.MeterRegistry;

@Path("/hello")
public class GreetingResource {

    @Inject
    MeterRegistry registry;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        registry.counter("greeting_counter").increment();
        return "Hello !!!";
    }
}