package hwu.elixir.WebApp.controller;

import java.util.HashSet;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleIRI;
import org.eclipse.rdf4j.model.impl.SimpleLiteral;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import hwu.elixir.dataAccess.TripleStoreWrapper;

@Controller
public class QueryDetails {

    private static Logger logger = LoggerFactory.getLogger(System.class.getName());

    private TripleStoreWrapper dbms = new TripleStoreWrapper();

    @RequestMapping("/inspect")
    public String query(Model model, @RequestParam String url, HttpServletRequest request) {
        TreeMap<String, HashSet<String>> details = new TreeMap<String, HashSet<String>>(String.CASE_INSENSITIVE_ORDER);

        logger.info("Querying details for URL: " + url);

        String sparql = "select distinct * where { <" + url + "> ?p ?o . }";
        details = processQuery(details, sparql);
        logger.info(sparql);

        sparql = "select distinct * where { ?s ?p <" + url + "> . }";
        details = processQuery(details, sparql);
        logger.info(sparql);

        sparql = "select distinct ?g ?source ?date " + "where { graph ?g { "
                + "?s ?p <"+url+"> . "
                + "?g <http://purl.org/pav/retrievedFrom> ?source . " + "?g <http://purl.org/pav/retrievedOn> ?date . "
                + "} }";
        logger.info(sparql);
        details = processMeta(details, sparql, url);


        // remove trailing -X (where X is a number up to 3 digits) that are added to some URLs to distinguish between nested elements
        // only necessary for URL param as it needs to point to the source page
        String endOfUrl = new StringBuffer(url.subSequence(url.length()-4, url.length())).toString();
        if(endOfUrl.contains("-")) {
            url = url.substring(0, url.lastIndexOf("-"));
        }

        model.addAttribute("url", url);
        model.addAttribute("details", details);

        return "details";
    }

    private TreeMap<String, HashSet<String>> processMeta(TreeMap<String, HashSet<String>> details, String sparql, String url) {
        TupleQueryResult result = dbms.query(sparql);

        if(!result.hasNext()) {
            sparql = "select distinct ?g ?source ?date " + "where { graph ?g { "
                    + "?s ?p \""+url+"\" . "
                    + "?g <http://purl.org/pav/retrievedFrom> ?source . " + "?g <http://purl.org/pav/retrievedOn> ?date . "
                    + "} }";
            logger.info(sparql);
            result = dbms.query(sparql);
        }

        if(!result.hasNext()) {
            sparql = "select distinct ?g ?source ?date " + "where { graph ?g { "
                    + "<"+url+"> ?p ?o. "
                    + "?g <http://purl.org/pav/retrievedFrom> ?source . " + "?g <http://purl.org/pav/retrievedOn> ?date . "
                    + "} }";
            logger.info(sparql);
            result = dbms.query(sparql);
        }

        while (result.hasNext()) {
            BindingSet bindingSet = result.next();
            Value date = bindingSet.getValue("date");
            Value source = bindingSet.getValue("source");

            HashSet<String> values = new HashSet<String>();
            values.add(date.stringValue());
            logger.info("pulledOn "+values.toString());
            details.put("pulledOn", values);

            values = new HashSet<String>();
            values.add(source.stringValue());
            logger.info("pulledFrom "+values.toString());
            details.put("pulledFrom", values);
        }

        return details;
    }

    private TreeMap<String, HashSet<String>> processQuery(TreeMap<String, HashSet<String>> details, String sparql) {
        TupleQueryResult result = dbms.query(sparql);

        while (result.hasNext()) {
            BindingSet bindingSet = result.next();
            Value predicate = bindingSet.getValue("p");
            Value object = bindingSet.getValue("o");

            if (predicate.stringValue().contains("ogp.me") && predicate.stringValue().contains("type")) {
                continue;
            }

            String predicateString = finalisePredicateName(predicate);

            if (object instanceof SimpleLiteral) {
                details = addToHashSet(details, object, predicateString);

            } else if (object instanceof SimpleIRI) {
                if (object.stringValue().startsWith("http://www.w3.org/")
                        || (object.stringValue().startsWith("https://www.w3.org/"))) {
                    continue;
                }

                details = addToHashSet(details, object, predicateString);
            }
        }
        result.close();

        return details;
    }

    private TreeMap<String, HashSet<String>> addToHashSet(TreeMap<String, HashSet<String>> details, Value object, String predicateString) {
        if (!details.containsKey(predicateString)) {
            HashSet<String> values = new HashSet<String>();
            values.add(object.stringValue());
            details.put(predicateString, values);
        } else {
            details.get(predicateString).add(object.stringValue());
        }
        return details;
    }

    private String finalisePredicateName(Value predicate) {
        String predString = predicate.stringValue();

        if (predString.startsWith("http")) {
            int cutPos = predString.lastIndexOf("#");

            if (cutPos == -1) {
                cutPos = predString.lastIndexOf("/");
            }

            if (cutPos != -1)
                predString = predString.substring(cutPos + 1);
        }

        return predString;
    }
}
