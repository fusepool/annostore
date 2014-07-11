package eu.fusepool.annostore;

import java.security.AccessController;
import java.security.Permission;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.Iterator;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import org.apache.clerezza.rdf.core.Graph;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.Triple;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.EntityAlreadyExistsException;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.core.access.security.TcAccessController;
import org.apache.clerezza.rdf.core.access.security.TcPermission;
import org.apache.clerezza.rdf.ontologies.DC;
import org.apache.clerezza.rdf.ontologies.RDF;
import org.apache.clerezza.rdf.utils.GraphNode;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.commons.security.UserUtil;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Allows user to store annotations.
 *
 * A user can store minimum self contained graphs which describe a resource of
 * type http://www.w3.org/ns/oa#Annotation. This service will add dc:creator and
 * dc:date properties.
 */
@Component
@Service(Object.class)
@Property(name = "javax.ws.rs", boolValue = true)
@Path("annostore")
public class AnnoStore {

    /**
     * Using slf4j for logging
     */
    private static final Logger log = LoggerFactory.getLogger(AnnoStore.class);
    private static final UriRef ANNOTATION_TYPE = new UriRef("http://www.w3.org/ns/oa#Annotation");
    /**
     * This service allows accessing and creating persistent triple collections
     */
    @Reference
    private TcManager tcManager;
    /**
     * This is the name of the graph in which we "log" the requests
     */
    private UriRef ANNOTATION_GRAPH_NAME = new UriRef("urn:x-localinstance:/fusepool/annotation.graph");

    @Activate
    protected void activate(ComponentContext context) {
        log.info("The example service is being activated");
        try {
            tcManager.createMGraph(ANNOTATION_GRAPH_NAME);
            //now make sure everybody can read from the graph
            //or more precisly, anybody who can read the content-graph
            TcAccessController tca = tcManager.getTcAccessController();
            tca.setRequiredReadPermissions(ANNOTATION_GRAPH_NAME,
                    Collections.singleton((Permission) new TcPermission(
                    "urn:x-localinstance:/content.graph", "read")));
        } catch (EntityAlreadyExistsException ex) {
            log.debug("The graph for the request log already exists");
        }

    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
    }

    /**
     * Returns the context of a resource. This includes any Annotation on that
     * resource.
     */
    @GET
    public GraphNode serviceEntry(@Context final UriInfo uriInfo,
            @QueryParam("iri") final UriRef iri) throws Exception {
        return new GraphNode(iri, getAnnotationGraph());
    }

    @POST
    public GraphNode postAnnotation(final Graph postedGraph) {
        final Iterator<Triple> typeTriples = postedGraph.filter(null, RDF.type, ANNOTATION_TYPE);
        if (!typeTriples.hasNext()) {
            final Response.ResponseBuilder rb = Response.status(Status.BAD_REQUEST);
            rb.entity("Graph does not contain an annotation");
            throw new WebApplicationException(rb.build());
        }
        final GraphNode postedNode = new GraphNode(
                typeTriples.next().getSubject(), postedGraph);
        if (typeTriples.hasNext()) {
            final Response.ResponseBuilder rb = Response.status(Status.BAD_REQUEST);
            rb.entity("Graph contains more than one annotation");
            throw new WebApplicationException(rb.build());
        }
        if (postedNode.getObjects(DC.creator).hasNext()) {
            final Response.ResponseBuilder rb = Response.status(Status.BAD_REQUEST);
            rb.entity("annotation has a DC:creator property. The annotation must "
                    + "not have such a property, this will be added by the system.");
            throw new WebApplicationException(rb.build());
        }
        final String userName = UserUtil.getCurrentUserName();
        if (userName == null) {
            final Response.ResponseBuilder rb = Response.status(Status.BAD_REQUEST);
            rb.entity("No username available");
            throw new WebApplicationException(rb.build());
        }
        return AccessController.doPrivileged(new PrivilegedAction<GraphNode>() {
            public GraphNode run() {
                getAnnotationGraph().addAll(postedNode.getNodeContext());
                final GraphNode storedNode = new GraphNode(
                        postedNode.getNode(), getAnnotationGraph());
                storedNode.addPropertyValue(DC.creator, userName);
                return storedNode;
            }
        });
    }

    /**
     * @return the MGraph for the annotation
     */
    private MGraph getAnnotationGraph() {
        return tcManager.getMGraph(ANNOTATION_GRAPH_NAME);
    }
}
