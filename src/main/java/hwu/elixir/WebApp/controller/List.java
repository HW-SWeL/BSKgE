package hwu.elixir.WebApp.controller;

import java.util.ArrayList;
import java.util.Optional;

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
public class List {

    private static Logger logger = LoggerFactory.getLogger(System.class.getName());

    private TripleStoreWrapper dbms = new TripleStoreWrapper();

    @RequestMapping(value = {"/list"})
    public String list(Model model, @RequestParam(value = "type") Optional<String> type) {

        String sparql;


        logger.info("TYPE present: " + type.isPresent());

        if (type.isPresent()) {
            String typeValue = type.get();
            logger.info("TYPE: " + type);

            if (typeValue.startsWith("http")) {
                sparql = "select distinct * where { ?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <" + typeValue + "> }";
            } else if (!typeValue.startsWith("https://schema.org/") && !typeValue.startsWith("http://schema.org/")) {
                sparql = "select distinct * where { ?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <https://schema.org/" + typeValue + "> }";
            } else {
                return "list";
            }
        } else {
            return "list";
        }

        model.addAttribute("type", type.get());

        logger.info("QUERY: " + sparql);

        model.addAttribute("query", sparql);

        TupleQueryResult result = dbms.query(sparql);
        ArrayList<String> allURLS = new ArrayList<String>();
        while (result.hasNext()) {
            BindingSet bindingSet = result.next();
            String iri = bindingSet.getValue("s").stringValue();

            if (iri.startsWith("htt")) {
                allURLS.add(iri);
            } else {
                logger.warn("There should be no blank nodes BUT some found for type = " + type);
            }
        }

        model.addAttribute("urls", allURLS);

        return "list";
    }

}
