package paprika.neo4j;

import net.sourceforge.jFuzzyLogic.FIS;
import net.sourceforge.jFuzzyLogic.FunctionBlock;
import org.neo4j.cypher.CypherException;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Geoffrey Hecht on 14/08/15.
 */
public class LMQuery extends FuzzyQuery{
    protected static double high = 17;
    protected static double veryHigh = 26;

    public LMQuery(QueryEngine queryEngine) {
        super(queryEngine);
        fclFile = "/LongMethod.fcl";
    }

    public void execute() throws CypherException, IOException {
        Result result;
        try (Transaction ignored = graphDatabaseService.beginTx()) {
            String query = "MATCH (m:Method) WHERE m.number_of_instructions >" + veryHigh + " RETURN m.app_key as app_key,count(m) as LM";
            result = graphDatabaseService.execute(query);
            queryEngine.resultToCSV(result,"_LM.csv");
        }
    }

    public void executeFuzzy() throws CypherException, IOException {
            Result result;
            try (Transaction ignored = graphDatabaseService.beginTx()) {
                String query =  "MATCH (m:Method) WHERE m.number_of_instructions >" + high + " RETURN m.app_key as app_key,m.number_of_instructions as number_of_instructions";
                result = graphDatabaseService.execute(query);
                List<String> columns = new ArrayList<>(result.columns());
                columns.add("fuzzy_value");
                int cc;
                List<Map> fuzzyResult = new ArrayList<>();
                File fcf = new File(fclFile);
                //We look if the file is in a directory otherwise we look inside the jar
                FIS fis;
                if(fcf.exists() && !fcf.isDirectory()){
                    fis = FIS.load(fclFile, false);
                }else{
                    fis = FIS.load(getClass().getResourceAsStream(fclFile),false);
                }
                FunctionBlock fb = fis.getFunctionBlock(null);
                while(result.hasNext()){
                    HashMap res = new HashMap(result.next());
                    cc = (int) res.get("number_of_instructions");
                    if(cc >= veryHigh){
                        res.put("fuzzy_value", 1);
                    }else {
                        fb.setVariable("number_of_instructions",cc);
                        fb.evaluate();
                        res.put("fuzzy_value", fb.getVariable("res").getValue());
                    }
                    fuzzyResult.add(res);
                    }
                    queryEngine.resultToCSV(fuzzyResult,columns,"_LM.csv");
            }
    }
}
