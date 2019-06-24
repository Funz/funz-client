package org.funz.doe;

import java.util.*;
import org.funz.doeplugin.Design;
import org.funz.doeplugin.DesignSession;
import org.funz.doeplugin.Designer;
import org.funz.parameter.OutputFunctionExpression;
import org.funz.parameter.Parameter;

/** simple dichotomy for zero search.
 *
 * @author Y. Richet
 *
 */
public class TESTIterativeDesigner extends Designer {

    final static String QUICKHELP = "Basic iterative designer";

    public TESTIterativeDesigner() {
    _opts.put("N", "5");
    }

    @Override
    public String getName() {
        return "Test iterative";
    }

    @Override
    public String getType() {
        return "TEST";
    }

    @Override
    public String getDesignOutputTitle() {
        return "Output values";
    }

    @Override
    public String getQuickHelp() {
        return QUICKHELP;
    }

    @Override
    public String isValid(List<Parameter> params, OutputFunctionExpression f) {
        StringBuffer answer = new StringBuffer();

        if (!(f instanceof OutputFunctionExpression.Numeric)) {
            answer.append("Output function must be numeric.\n");
        }

        if (params.size() <= 0) {
            answer.append("Needs at least 1 parameter.\n");
        }

        return answer.length() == 0 ? VALID : answer.toString();
    }

    

    @Override
    public Design createDesign(DesignSession ds) {
        return new TESTIterative(this, ds);
    }

    @Override
    public boolean viewManagedParams() {
        return true;
    }
}
