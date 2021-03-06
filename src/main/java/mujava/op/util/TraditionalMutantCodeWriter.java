/**
 * Copyright (C) 2015  the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */ 


/**
 * <p>Description: </p>
 * @author Jeff Offutt and Yu-Seung Ma
 * @version 1.0
  */ 

package mujava.op.util;

import java.io.*;
import mujava.MutationSystem;
import openjava.ptree.BinaryExpression;
import openjava.ptree.ParseTree;
import openjava.ptree.ParseTreeException;
import openjava.ptree.UnaryExpression;


public class TraditionalMutantCodeWriter extends MutantCodeWriter{

       String method_signature = null;

    public TraditionalMutantCodeWriter( PrintWriter out ) {
        super(out);
    }

    public TraditionalMutantCodeWriter( String mutant_dir, PrintWriter out ) {
        super(mutant_dir,out);
    }

    public void setMethodSignature(String str){
      method_signature = str;
    }

    protected void writeLog(String changed_content)
    {
        CodeChangeLog.writeLog(class_name+ MutationSystem.LOG_IDENTIFIER
                + mutated_line+MutationSystem.LOG_IDENTIFIER
                + method_signature + MutationSystem.LOG_IDENTIFIER
                + changed_content.substring(changed_content.lastIndexOf(MutationSystem.LOG_IDENTIFIER) + 1));

        FullCodeChangeLog.writeLog(class_name
                + MutationSystem.LOG_IDENTIFIER + classPackage
                + MutationSystem.LOG_IDENTIFIER + mutated_line
                + MutationSystem.LOG_IDENTIFIER + method_signature
                + MutationSystem.LOG_IDENTIFIER + changed_content);
    }

    protected static String appendTargetInfo(ParseTree p, String content) {
        return p.getObjectID() + MutationSystem.LOG_IDENTIFIER +
                p.getClass().getSimpleName() + MutationSystem.LOG_IDENTIFIER
                + content;
    }

    protected static String appendTargetInfo(ParseTree p, String content, String label, String originalId) {

        return p.getObjectID()
                + MutationSystem.LOG_IDENTIFIER + p.getClass().getSimpleName()
                + MutationSystem.LOG_IDENTIFIER + label
                + MutationSystem.LOG_IDENTIFIER + originalId
                + MutationSystem.LOG_IDENTIFIER + getOperator(p)
                + MutationSystem.LOG_IDENTIFIER + isPrefix(p)
                + MutationSystem.LOG_IDENTIFIER + composedBy(p)
                + MutationSystem.LOG_IDENTIFIER + content;
    }

    private static String getOperator(ParseTree p) {
        String operator = "";

        if (p instanceof BinaryExpression) {
            operator = ((BinaryExpression) p).operatorString();
        }

        if (p instanceof UnaryExpression) {
            operator = ((UnaryExpression) p).operatorString();
        }

        return operator;
    }

    private static boolean isPrefix(ParseTree p) {
        boolean isPrefix= false;

        if (p instanceof UnaryExpression) {
            isPrefix = ((UnaryExpression) p).isPrefix();
        }

        return isPrefix;
    }

    private static String composedBy(ParseTree p) {
        StringBuilder children = new StringBuilder();

        try {
            ComposedStatementVisitor visitor = new ComposedStatementVisitor();
            p.accept(visitor);
            for (ParseTree c : visitor.getChildren()) {
                children.append(c.getObjectID());
                children.append(",");
            }
            if (children.lastIndexOf(",") > -1) {
                children.deleteCharAt(children.lastIndexOf(","));
            }
        } catch (ParseTreeException e) {
            e.printStackTrace();
        }

        return children.toString();
    }


}