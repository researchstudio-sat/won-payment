package won.payment.paypal.bot.util;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

public class InformationExtractor {

	private static final String paymentDetailsQuery;
	
	private static final String PAYMENT = "main";
    private static final String AMOUNT = "amount";
    private static final String CURRENCY = "currency";
    private static final String RECEIVER = "receiver";
    private static final String SECRET = "secret";
    private static final String FEEPAYER = "feepayer";
    private static final String TAX = "tax";
    private static final String INVOICEID = "invoiceid";
    private static final String INVOICEDETAILS = "invoicedetails";
    private static final String EXPIRATIONTIME = "expirationtime";
    
    
    
    static {
    	paymentDetailsQuery = ResourceManager.getResourceAsString("/temp/paymentDetails.rq");
    }
    
    public static Resource getPayment(Model payload) {
    	if(payload != null) {
            QuerySolution solution = executeQuery(paymentDetailsQuery, payload);

            if (solution != null) {
                return solution.getResource(PAYMENT);
            }
        }
        return null;
    }
    
    public static Double getAmount(Model payload) {
    	if(payload != null) {
            QuerySolution solution = executeQuery(paymentDetailsQuery, payload);

            if (solution != null) {
                return solution.getLiteral(AMOUNT).getDouble();
            }
        }
        return null;
    }
    
    public static String getCurrency(Model payload) {
    	if(payload != null) {
            QuerySolution solution = executeQuery(paymentDetailsQuery, payload);

            if (solution != null) {
                return solution.getLiteral(CURRENCY).getString();
            }
        }
        return null;
    }
    
    public static String getReceiver(Model payload) {
    	if(payload != null) {
            QuerySolution solution = executeQuery(paymentDetailsQuery, payload);

            if (solution != null) {
                return solution.getLiteral(RECEIVER).getString();
            }
        }
        return null;
    }
    
    public static String getSecret(Model payload) {
    	if(payload != null) {
            QuerySolution solution = executeQuery(paymentDetailsQuery, payload);

            if (solution != null) {
                return solution.getLiteral(SECRET).getString();
            }
        }
        return null;
    }

    // Optional
    // TODO: verify this works after buyer interaction removal
    public static Resource getFeePayer(Model payload) {
    	if(payload != null) {
            QuerySolution solution = executeQuery(paymentDetailsQuery, payload);

            if (solution != null && solution.getResource(FEEPAYER) != null) {
                return solution.getResource(FEEPAYER);
            }
        }
        return null;
    }
    
    public static Double getTax(Model payload) {
    	if(payload != null) {
            QuerySolution solution = executeQuery(paymentDetailsQuery, payload);

            if (solution != null && solution.getLiteral(TAX) != null) {
                return solution.getLiteral(TAX).getDouble();
            }
        }
        return null;
    }
    
    public static String getInvoiceId(Model payload) {
    	if(payload != null) {
            QuerySolution solution = executeQuery(paymentDetailsQuery, payload);

            if (solution != null && solution.getLiteral(INVOICEID) != null) {
                return solution.getLiteral(INVOICEID).getString();
            }
        }
        return null;
    }
    
    public static String getInvoiceDetails(Model payload) {
    	if(payload != null) {
            QuerySolution solution = executeQuery(paymentDetailsQuery, payload);

            if (solution != null && solution.getLiteral(INVOICEDETAILS) != null) {
                return solution.getLiteral(INVOICEDETAILS).getString();
            }
        }
        return null;
    }
    
    public static String getExpirationTime(Model payload) {
    	if(payload != null) {
            QuerySolution solution = executeQuery(paymentDetailsQuery, payload);

            if (solution != null && solution.getLiteral(EXPIRATIONTIME) != null) {
                return solution.getLiteral(EXPIRATIONTIME).getString();
            }
        }
        return null;
    }
    
    
    
    
    
    
    private static QuerySolution executeQuery(String queryString, Model payload) {
        Query query = QueryFactory.create(queryString);
        try(QueryExecution qexec = QueryExecutionFactory.create(query, payload)){
            ResultSet resultSet = qexec.execSelect();
            if (resultSet.hasNext()){
                QuerySolution solution = resultSet.nextSolution();
                return solution;
            }
        }
        return null;
    }
}
