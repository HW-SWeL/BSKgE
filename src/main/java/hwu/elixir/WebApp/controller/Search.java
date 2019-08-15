package hwu.elixir.WebApp.controller;

import java.util.HashMap;
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
public class Search {

    private static Logger logger = LoggerFactory.getLogger(Search.class);

    private TripleStoreWrapper dbms = new TripleStoreWrapper();

    @RequestMapping(value = {"/search"})
    public String list(Model model, @RequestParam(value = "term") Optional<String> term) {

        String sparql;
        String sparql2;
        String termValue = "";
        if (term.isPresent()) {
            termValue = term.get();
            logger.info("SEARCH TERM: " + termValue);
            if (termValue.equalsIgnoreCase("")) {
                model.addAttribute("queryTerm", "You did not supply a query term!");
                return "searchResults";
            }

            model.addAttribute("queryTerm", termValue);

            sparql = "SELECT DISTINCT ?s ?o WHERE { " + "{?s <https://schema.org/name> ?o . FILTER regex(?o, \""
                    + termValue + "\") } union " + "{?s <https://schema.org/identifier> ?o . FILTER regex(?o, \""
                    + termValue + "\") } union " + "{?s <https://schema.org/description> ?o . FILTER regex(?o, \""
                    + termValue + "\") } union " + "{?s <https://schema.org/title> ?o . FILTER regex(?o, \"" + termValue
                    + "\") } union " + "{?s <https://schema.org/keywords> ?o . FILTER regex(?o, \"" + termValue
                    + "\") } } ";

            sparql2 = "SELECT DISTINCT ?s ?o " + "WHERE { " + "{?s <https://schema.org/name> ?o . ?o bif:contains \"'"
                    + termValue + "*'\". } union " + "{?s <https://schema.org/identifier> ?o . ?o bif:contains \"'"
                    + termValue + "*'\". } union " + "{?s <https://schema.org/description> ?o . ?o bif:contains \"'"
                    + termValue + "*'\". } union " + "{?s <https://schema.org/title> ?o . ?o bif:contains \"'"
                    + termValue + "*'\". } union " + "{?s <https://schema.org/keywords> ?o . ?o bif:contains \"'"
                    + termValue + "'\". }  }";
        } else {
            model.addAttribute("queryTerm", "You did not supply a query term!");
            return "searchResults";
        }

        logger.info("QUERY: " + sparql);
        logger.info("QUERY2: " + sparql2);

        TupleQueryResult result = null;
        try {
            result = dbms.query(sparql);
        } catch (Exception ve) {
            logger.info("query 1 threw an exception");
        }

        if (result == null || !result.hasNext()) {
            try {
                logger.info("trying QUERY 2");
                result = dbms.query(sparql2);
            } catch (Exception e) {
                logger.info("QUERY 2 threw an exception");
                return "searchResults";
            }
        }

        if (result == null || !result.hasNext())
            return "searchResults";

        HashMap<String, String> results = new HashMap<String, String>();
        while (result.hasNext()) {
            BindingSet bindingSet = result.next();
            String s = bindingSet.getValue("s").stringValue();
            String o = bindingSet.getValue("o").stringValue();

            o = o.replaceAll(termValue, "<b>" + termValue + "</b>");

            o = processResultForOutput(o, termValue);

            if (s.startsWith("htt")) {
                results.put(s, o);
            } else {
                logger.warn("There should be no blank nodes BUT some found for type = " + term);
            }
        }

        model.addAttribute("results", results);

        return "searchResults";
    }

    private String processResultForOutput(String result, String term) {
        if (result.length() > 100) {
            int counter = 0;
            String[] array = result.split(" +");
            for (String s1 : array) {
                String tempCompare = s1.toLowerCase().replace("<b>", "").replace("</b>", "").replace(".", "").trim();
                if (term.equalsIgnoreCase(tempCompare) || tempCompare.contains(term.toLowerCase())) {
                    break;
                }
                counter++;
            }

            boolean reachedStart = false;
            boolean reachedEnd = false;
            String temp = array[counter];
            int another = 1;
            while (temp.length() < 100) {
                try {
                    temp = array[counter - another] + " " + temp;
                } catch (ArrayIndexOutOfBoundsException e) {
                    reachedStart = true;
                }
                try {
                    temp += " " + array[counter + another];
                } catch (ArrayIndexOutOfBoundsException e) {
                    reachedEnd = true;
                }
                another++;
            }

            if (!reachedStart)
                temp = "... " + temp;
            if (!reachedEnd)
                temp += "... ";

            return temp;
        }
        return result;
    }

}
