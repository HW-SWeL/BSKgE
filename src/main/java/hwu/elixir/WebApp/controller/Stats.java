package hwu.elixir.WebApp.controller;

import java.util.HashMap;
import java.util.LinkedHashMap;

import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import hwu.elixir.dataAccess.TripleStoreWrapper;

@Controller
public class Stats {

    private static Logger logger = LoggerFactory.getLogger(Stats.class);

    private TripleStoreWrapper dbms = new TripleStoreWrapper();

    @RequestMapping(value = {"/stats"})
    public String list(Model model) {

        String sparql = "SELECT (COUNT(DISTINCT ?s) AS ?triples) WHERE { ?s ?p ?o }";
        TupleQueryResult result = dbms.query(sparql);
        logger.info("QUERY: " + sparql);
        if (result.hasNext()) {
            BindingSet bindingSet = result.next();
            String number = bindingSet.getValue("triples").stringValue();

            model.addAttribute("totalNumberTriples", number);
        }

        // number graphs = number of pages pulled
        sparql = "SELECT (COUNT(DISTINCT ?g) AS ?numGraphs) WHERE { ?g <http://purl.org/pav/retrievedFrom> ?source . }";
        result = dbms.query(sparql);
        logger.info("QUERY: " + sparql);
        if (result.hasNext()) {
            BindingSet bindingSet = result.next();
            String number = bindingSet.getValue("numGraphs").stringValue();

            model.addAttribute("totalNumberGraphs", number);
        }


        sparql = "SELECT ?type (COUNT(DISTINCT ?s) as ?number) "
                + "where {?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?type. } " + "group by ?type  order by ?type";
        result = dbms.query(sparql);
        logger.info("QUERY: " + sparql);

        LinkedHashMap<String, String> allTypes = new LinkedHashMap<String, String>();
        while (result.hasNext()) {
            BindingSet bindingSet = result.next();

            String type = bindingSet.getValue("type").stringValue();
            String number = bindingSet.getValue("number").stringValue();
            if (type.contains("MolecularEntity") ||
                    type.contains("Sample") ||
//                    type.contains("BioChemEntity") ||
                    type.contains("Dataset") ||
                    type.contains("DataCatalog")) {
                allTypes.put(type.replaceFirst("https://schema.org/", ""), number);
            }
        }

        if (allTypes.size() > 0) {
            model.addAttribute("typeCount", allTypes);
        }

        return "statistics";
    }

}
