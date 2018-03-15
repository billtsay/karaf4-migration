package org.code_house.swagger.core.internal;

import io.swagger.annotations.ApiOperation;
import io.swagger.config.FilterFactory;
import io.swagger.core.filter.SpecFilter;
import io.swagger.core.filter.SwaggerSpecFilter;
import io.swagger.jaxrs.listing.ApiListingResource;
import io.swagger.models.Swagger;
import io.swagger.util.Yaml;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.*;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/swagger.{type:json|yaml}")
public class EnhancedApiListingResource extends ApiListingResource {

    private final SwaggerHolder holder;

    public EnhancedApiListingResource(SwaggerHolder holder) {
        this.holder = holder;
    }

    @Context
    ServletContext context;

    @GET
    @Produces({MediaType.APPLICATION_JSON, "application/yaml"})
    @ApiOperation(value = "The swagger definition in either JSON or YAML", hidden = true)
    public Response getListing(
            @Context Application app,
            @Context ServletConfig sc,
            @Context HttpHeaders headers,
            @Context UriInfo uriInfo,
            @PathParam("type") String type) {
        if (StringUtils.isNotBlank(type) && type.trim().equalsIgnoreCase("yaml")) {
            return getListingYaml(app, sc, headers, uriInfo);
        } else {
            return getListingJson(app, sc, headers, uriInfo);
        }
    }

    public Response getListingJson(
            Application app,
            ServletConfig sc,
            HttpHeaders headers,
            UriInfo uriInfo) {
        Swagger swagger = holder.getSwagger();
        if (swagger != null) {
            SwaggerSpecFilter filterImpl = FilterFactory.getFilter();
            if (filterImpl != null) {
                SpecFilter f = new SpecFilter();
                swagger = f.filter(swagger,
                        filterImpl,
                        getQueryParams(uriInfo.getQueryParameters()),
                        getCookies(headers),
                        getHeaders(headers));
            }
            return Response.ok().entity(swagger).build();
        } else {
            return Response.status(404).build();
        }
    }

    public Response getListingYaml(
            Application app,
            ServletConfig sc,
            HttpHeaders headers,
            UriInfo uriInfo) {

        Swagger swagger = holder.getSwagger();
        try {
            if (swagger != null) {
                SwaggerSpecFilter filterImpl = FilterFactory.getFilter();
                if (filterImpl != null) {
                    SpecFilter f = new SpecFilter();
                    swagger = f.filter(swagger,
                            filterImpl,
                            getQueryParams(uriInfo.getQueryParameters()),
                            getCookies(headers),
                            getHeaders(headers));
                }

                String yaml = Yaml.mapper().writeValueAsString(swagger);
                String[] parts = yaml.split("\n");
                StringBuilder b = new StringBuilder();
                for (String part : parts) {
                    int pos = part.indexOf("!<");
                    int endPos = part.indexOf(">");
                    b.append(part);
                    b.append("\n");
                }
                return Response.ok().entity(b.toString()).type("application/yaml").build();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Response.status(404).build();
    }

    private static Map<String, List<String>> getQueryParams(MultivaluedMap<String, String> params) {
        Map<String, List<String>> output = new HashMap<String, List<String>>();
        if (params != null) {
            for (String key : params.keySet()) {
                List<String> values = params.get(key);
                output.put(key, values);
            }
        }
        return output;
    }

    private static Map<String, String> getCookies(HttpHeaders headers) {
        Map<String, String> output = new HashMap<String, String>();
        if (headers != null) {
            for (String key : headers.getCookies().keySet()) {
                Cookie cookie = headers.getCookies().get(key);
                output.put(key, cookie.getValue());
            }
        }
        return output;
    }

    private static Map<String, List<String>> getHeaders(HttpHeaders headers) {
        Map<String, List<String>> output = new HashMap<String, List<String>>();
        if (headers != null) {
            for (String key : headers.getRequestHeaders().keySet()) {
                List<String> values = headers.getRequestHeaders().get(key);
                output.put(key, values);
            }
        }
        return output;
    }

}
