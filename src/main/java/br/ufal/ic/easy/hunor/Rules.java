package br.ufal.ic.easy.hunor;

import mujava.op.basic.Arithmetic_OP;
import mujava.op.basic.MethodLevelMutator;
import openjava.mop.OJSystem;
import openjava.ptree.*;


import java.io.File;
import java.util.*;
import java.util.List;

public class Rules {

    public enum Mutation {
        AORB_PLUS, AORB_MINUS, AORB_TIMES, AORB_DIVIDE, AORB_MOD,
        CDL_LEXP, CDL_REXP, CDL_EXP,
        VDL_LEXP, VDL_REXP, VDL_EXP,
        ODL_LEXP, ODL_REXP, ODL_EXP, ODL_ASSING,
        AOIU_MINUS,
        AOIS_PREINC, AOIS_PREDEC, AOIS_POSINC, AOIS_POSDEC,
        LOI_BITNOT,
        COI_NOT,
        COR_AND, COR_OR, COR_XOR, COR_EQUAL, COR_NOTEQUAL, COR_TRUE, COR_FALSE,
        ROR_GREATER, ROR_GREATEREQUAL, ROR_LESS, ROR_LESSEQUAL, ROR_EQUAL,
        ROR_NOTEQUAL, ROR_TRUE, ROR_FALSE,
        LOR_BITOR, LOR_BITAND, LOR_BITXOR,
        LOD,
        COD,
        AODU,
        AODS,
        AORS_PREINC, AORS_PREDEC, AORS_POSINC, AORS_POSDEC,
        ASRS_ADD, ASRS_SUB, ASRS_MUL, ASRS_DIV, ASRS_MOD, ASRS_RSH, ASRS_LSH,
        ASRS_URS, ASRS_AND, ASRS_OR, ASRS_XOR
    }

    private static boolean enabled = false;
    private static boolean newMutationsEnabled = false;
    private static List<String> traditionalOperatorsEnabled = Collections.emptyList();
    private static File currentFile;
    private static MethodLevelMutator mutator;
    private static Map<File, Map<Integer, Set<Mutation>>> fileTargets = new HashMap<>();


    private static boolean operatorDisabled(String operator) {
        return !traditionalOperatorsEnabled.contains(operator);
    }

    private static boolean operatorEnabled(String operador) {
        return !operatorDisabled(operador);
    }

    public static void setTraditionalOperatorsEnabled(String[] operators) {
        traditionalOperatorsEnabled = Arrays.asList(operators);
    }

    public static boolean canApply(ParseTree target, Mutation mutation, MethodLevelMutator m) {
        if (!enabled) return true;

        mutator = m;
        Optional<Set<Mutation>> allowedMutations = getAllowedMutations(target);

        return allowedMutations.map(mutations -> mutations.contains(mutation)).orElse(true);
    }

    private static Optional<Set<Mutation>> getAllowedMutations(ParseTree target) {
        Map<Integer, Set<Mutation>> currentFileTargets = getCurrentTargets();

        if (!currentFileTargets.keySet().contains(target)) {
            Optional<Set<Mutation>> allowedMutations = allowedMutationsFor(target);

            if (allowedMutations.isPresent()) {
                currentFileTargets.put(target.getObjectID(), allowedMutations.get());
            } else {
                return Optional.empty();
            }
        }

        return Optional.of(currentFileTargets.get(target.getObjectID()));
    }

    private static Map<Integer, Set<Mutation>> getCurrentTargets() {
        if (!fileTargets.keySet().contains(currentFile)) {
            fileTargets.put(currentFile, new HashMap<>());
        }

        return fileTargets.get(currentFile);
    }

    private static Optional<Set<Mutation>> allowedMutationsFor(ParseTree target) {
        if (target instanceof BinaryExpression) {
            return allowedMutationsFor((BinaryExpression) target);
        } else if (target instanceof UnaryExpression) {
            return allowedMutationsFor((UnaryExpression) target);
        } else if (target instanceof Variable || target instanceof FieldAccess) {
            return allowedMutationsFor((Expression) target);
        } else if (target instanceof AssignmentExpression) {
            return allowedMutationsFor((AssignmentExpression) target);
        }

        return Optional.empty();
    }

    private static Optional<Set<Mutation>> allowedMutationsFor(BinaryExpression target) {
        int operator = target.getOperator();

        if (operator == BinaryExpression.PLUS) {
            return Optional.of(targetLExpPlusRExp(target));
        } else if (operator == BinaryExpression.MINUS) {
            return Optional.of(targetLExpMinusRExp(target));
        } else if (operator == BinaryExpression.TIMES) {
            return Optional.of(targetLExpTimesRExp(target));
        } else if (operator == BinaryExpression.DIVIDE) {
            return Optional.of(targetLExpDivideRExp(target));
        } else if (operator == BinaryExpression.MOD) {
            return Optional.of(targetLExpModRExp(target));
        } else if (operator == BinaryExpression.LOGICAL_AND) {
            return Optional.of(targetLExpLogicalAndRExp(target));
        } else if (operator == BinaryExpression.LOGICAL_OR) {
            return Optional.of(targetLExpLogicalOrRExp(target));
        } else if (operator == BinaryExpression.XOR && hasBooleanType(target)) {
            return Optional.of(targetLExpXorRExp(target));
        } else if (operator == BinaryExpression.GREATER) {
            return Optional.of(targetLExpGreaterRExp(target));
        } else if (operator == BinaryExpression.GREATEREQUAL) {
            return Optional.of(targetLExpGreaterEqualRExp(target));
        } else if (operator == BinaryExpression.LESS) {
            return Optional.of(targetLExpLessRExp(target));
        } else if (operator == BinaryExpression.LESSEQUAL) {
            return Optional.of(targetLExpLessEqualRExp(target));
        } else if (operator == BinaryExpression.EQUAL && hasArithmeticType(target)) {
            return Optional.of(targetLExpEqualRExp(target));
        } else if (operator == BinaryExpression.NOTEQUAL && hasArithmeticType(target)) {
            return Optional.of(targetLExpNotEqualRExp(target));
        } else if (operator == BinaryExpression.EQUAL && hasBooleanType(target)) {
            return Optional.of(targetLExpEqualRExpBoolean(target));
        } else if (operator == BinaryExpression.NOTEQUAL && hasBooleanType(target)) {
            return Optional.of(targetLExpNotEqualRExpBoolean(target));
        } else if (operator == BinaryExpression.EQUAL && !hasArithmeticType(target)) {
            return Optional.of(targetLExpEqualObjectRExp(target));
        } else if (operator == BinaryExpression.NOTEQUAL && !hasArithmeticType(target)) {
            return Optional.of(targetLExpNotEqualObjectRExp(target));
        } else if (operator == BinaryExpression.BITAND) {
            return Optional.of(targetLExpBitAndRExp(target));
        } else if (operator == BinaryExpression.BITOR) {
            return Optional.of(targetLExpBitOrRExp(target));
        } else if (operator == BinaryExpression.XOR && hasArithmeticType(target)) {
            return Optional.of(targetLExpBitXorRExp(target));
        }

        return Optional.empty();
    }

    private static Optional<Set<Mutation>> allowedMutationsFor(UnaryExpression target) {
        int operator = target.getOperator();

        if (operator == UnaryExpression.PLUS) {
            return Optional.of(targetPlusExp(target));
        } else if (operator == UnaryExpression.MINUS) {
            return Optional.of(targetMinusExp(target));
        } else if (operator == UnaryExpression.NOT) {
            return Optional.of(targetNotExp(target));
        } else if (operator == UnaryExpression.BIT_NOT) {
            return Optional.of(targetBitNotExp(target));
        } else if (operator == UnaryExpression.PRE_INCREMENT) {
            return Optional.of(targetPreIncExp(target));
        } else if (operator == UnaryExpression.PRE_DECREMENT) {
            return Optional.of(targetPreDecExp(target));
        } else if (operator == UnaryExpression.POST_INCREMENT) {
            return Optional.of(targetPosIncExp(target));
        } else if (operator == UnaryExpression.POST_DECREMENT) {
            return Optional.of(targetPosDecExp(target));
        }

        return Optional.empty();
    }

    private static Optional<Set<Mutation>> allowedMutationsFor(Expression target) {
        if (hasArithmeticType(target)) {
            return Optional.of(targetArithmeticExp());
        } else if (hasBooleanType(target)) {
            return Optional.of(targetBooleanExp());
        }

        return Optional.empty();
    }

    private static Optional<Set<Mutation>> allowedMutationsFor(AssignmentExpression target) {
        int operator = target.getOperator();

        if (operator == AssignmentExpression.ADD) {
            return Optional.of(targetAssignmentAdd(target));
        } else if (operator == AssignmentExpression.SUB) {
            return Optional.of(targetAssignmentSub(target));
        } else if (operator == AssignmentExpression.MULT) {
            return Optional.of(targetAssignmentMul(target));
        } else if (operator == AssignmentExpression.DIVIDE) {
            return Optional.of(targetAssignmentDiv(target));
        } else if (operator == AssignmentExpression.MOD) {
            return Optional.of(targetAssignmentMod(target));
        } else if (operator == AssignmentExpression.SHIFT_R) {
            return Optional.of(targetAssignmentShiftRight(target));
        } else if (operator == AssignmentExpression.SHIFT_L) {
            return Optional.of(targetAssignmentShiftLeft(target));
        } else if (operator == AssignmentExpression.SHIFT_RR) {
            return Optional.of(targetAssignmentUnsignedShitRight(target));
        } else if (operator == AssignmentExpression.AND) {
            return Optional.of(targetAssignmentAnd(target));
        } else if (operator == AssignmentExpression.OR) {
            return Optional.of(targetAssignmentOr(target));
        } else if (operator == AssignmentExpression.XOR) {
            return Optional.of(targetAssignmentXor(target));
        }

        return Optional.empty();
    }

    private static Set<Mutation> targetAssignmentAdd(AssignmentExpression target) {
        Set<Mutation> mutations = new HashSet<>();

        mutations.add(Mutation.ASRS_SUB);
        mutations.add(Mutation.ASRS_MOD);
        mutations.add(Mutation.ODL_ASSING);

        return mutations;
    }

    private static Set<Mutation> targetAssignmentSub(AssignmentExpression target) {
        Set<Mutation> mutations = new HashSet<>();

        mutations.add(Mutation.ASRS_ADD);
        mutations.add(Mutation.ASRS_MOD);
        mutations.add(Mutation.ODL_ASSING);

        return mutations;
    }

    private static Set<Mutation> targetAssignmentMul(AssignmentExpression target) {
        Set<Mutation> mutations = new HashSet<>();

        mutations.add(Mutation.ASRS_DIV);
        mutations.add(Mutation.ASRS_MOD);
        mutations.add(Mutation.ODL_ASSING);


        return mutations;
    }

    private static Set<Mutation> targetAssignmentDiv(AssignmentExpression target) {
        Set<Mutation> mutations = new HashSet<>();

        mutations.add(Mutation.ASRS_MUL);
        mutations.add(Mutation.ASRS_MOD);
        mutations.add(Mutation.ODL_ASSING);

        return mutations;
    }

    private static Set<Mutation> targetAssignmentMod(AssignmentExpression target) {
        Set<Mutation> mutations = new HashSet<>();

        mutations.add(Mutation.ASRS_ADD);
        mutations.add(Mutation.ASRS_SUB);
        mutations.add(Mutation.ASRS_MUL);
        mutations.add(Mutation.ASRS_DIV);

        return mutations;
    }

    private static Set<Mutation> targetAssignmentShiftRight(AssignmentExpression target) {
        Set<Mutation> mutations = new HashSet<>();

        mutations.add(Mutation.ASRS_LSH);
        mutations.add(Mutation.ODL_ASSING);

        return mutations;
    }

    private static Set<Mutation> targetAssignmentShiftLeft(AssignmentExpression target) {
        Set<Mutation> mutations = new HashSet<>();

        mutations.add(Mutation.ASRS_RSH);

        return mutations;
    }

    private static Set<Mutation> targetAssignmentUnsignedShitRight(AssignmentExpression target) {
        Set<Mutation> mutations = new HashSet<>();

        mutations.add(Mutation.ASRS_RSH);
        mutations.add(Mutation.ASRS_LSH);

        return mutations;
    }

    private static Set<Mutation> targetAssignmentAnd(AssignmentExpression target) {
        Set<Mutation> mutations = new HashSet<>();

        mutations.add(Mutation.ODL_ASSING);

        return mutations;
    }

    private static Set<Mutation> targetAssignmentOr(AssignmentExpression target) {
        Set<Mutation> mutations = new HashSet<>();

        mutations.add(Mutation.ODL_ASSING);
        mutations.add(Mutation.ASRS_XOR);

        return mutations;
    }

    private static Set<Mutation> targetAssignmentXor(AssignmentExpression target) {
        Set<Mutation> mutations = new HashSet<>();

        mutations.add(Mutation.ASRS_OR);

        return mutations;
    }

    private static Set<Mutation> targetArithmeticExp() {
        Set<Mutation> mutations = new HashSet<>();

        mutations.add(Mutation.AOIU_MINUS);

        return mutations;
    }

    private static Set<Mutation> targetBooleanExp() {
        Set<Mutation> mutations = new HashSet<>();

        mutations.add(Mutation.COI_NOT);

        return mutations;
    }

    private static Set<Mutation> targetPlusExp(UnaryExpression target) {
        Set<Mutation> mutations = new HashSet<>();

        setMutationsTo(target.getExpression(), Collections.singleton(Mutation.LOI_BITNOT));

        return mutations;
    }

    private static Set<Mutation> targetMinusExp(UnaryExpression target) {
        Set<Mutation> mutations = new HashSet<>();

        setMutationsTo(target.getExpression(), Collections.emptySet());

        if (operatorEnabled("AODU")) {
            mutations.add(Mutation.AODU);
        } else if (operatorEnabled("ODL")) {
            mutations.add(Mutation.ODL_EXP);
        }

        return mutations;
    }

    private static Set<Mutation> targetPreIncExp(UnaryExpression target) {
        Set<Mutation> mutations = new HashSet<>();

        setMutationsTo(target.getExpression(), Collections.emptySet());

        if (operatorEnabled("AODS")) {
            mutations.add(Mutation.AODS);
        } else if (operatorEnabled("AORS")) {
            mutations.add(Mutation.AORS_POSDEC);
        } else if (operatorEnabled("ODL")) {
            mutations.add(Mutation.ODL_EXP);
        }

        return mutations;
    }

    private static Set<Mutation> targetPreDecExp(UnaryExpression target) {
        Set<Mutation> mutations = new HashSet<>();

        setMutationsTo(target.getExpression(), Collections.emptySet());

        if (operatorEnabled("AODS")) {
            mutations.add(Mutation.AODS);
        } else if (operatorEnabled("AORS")) {
            mutations.add(Mutation.AORS_POSINC);
        } else if (operatorEnabled("ODL")) {
            mutations.add(Mutation.ODL_EXP);
        }

        return mutations;
    }

    private static Set<Mutation> targetPosIncExp(UnaryExpression target) {
        Set<Mutation> mutations = new HashSet<>();

        setMutationsTo(target.getExpression(), Collections.singleton(Mutation.LOI_BITNOT));

        return mutations;
    }

    private static Set<Mutation> targetPosDecExp(UnaryExpression target) {
        Set<Mutation> mutations = new HashSet<>();

        setMutationsTo(target.getExpression(), Collections.singleton(Mutation.LOI_BITNOT));

        return mutations;
    }

    private static Set<Mutation> targetNotExp(UnaryExpression target) {
        Set<Mutation> mutations = new HashSet<>();

        setMutationsTo(target.getExpression(), Collections.emptySet());

        if (operatorEnabled("COD")) {
            mutations.add(Mutation.COD);
        } else if (operatorEnabled("ODL")) {
            mutations.add(Mutation.ODL_EXP);
        }

        return mutations;
    }

    private static Set<Mutation> targetBitNotExp(UnaryExpression target) {
        Set<Mutation> mutations = new HashSet<>();

        setMutationsTo(target.getExpression(), Collections.emptySet());

        if (operatorEnabled("LOD")) {
            mutations.add(Mutation.LOD);
        } else if (operatorEnabled("ODL")) {
            mutations.add(Mutation.ODL_EXP);
        } else {
            setMutationsTo(target.getExpression(), Collections.singleton(Mutation.LOI_BITNOT));
        }

        return mutations;
    }

    private static Set<Mutation> targetLExpPlusRExp(BinaryExpression target) {
        Set<Mutation> mutations = new HashSet<>();

        mutations.add(Mutation.AORB_MOD);

        if (operatorEnabled("ODL")) {
            mutations.add(Mutation.ODL_LEXP);
            mutations.add(Mutation.ODL_REXP);
        } else if (operatorEnabled("VDL") || operatorEnabled("CDL")) {
            mutations.add(Mutation.VDL_LEXP);
            mutations.add(Mutation.VDL_REXP);
            mutations.add(Mutation.CDL_LEXP);
            mutations.add(Mutation.CDL_REXP);
        } else {
            mutations.add(Mutation.AORB_MINUS);
        }

      return mutations;
    }

    private static Set<Mutation> targetLExpMinusRExp(BinaryExpression target) {
        Set<Mutation> mutations = new HashSet<>();

        mutations.add(Mutation.AORB_MOD);

        if (operatorEnabled("ODL")) {
            mutations.add(Mutation.ODL_LEXP);
            mutations.add(Mutation.ODL_REXP);
        } else if (operatorEnabled("VDL") || operatorEnabled("CDL")) {
            mutations.add(Mutation.VDL_LEXP);
            mutations.add(Mutation.VDL_REXP);
            mutations.add(Mutation.CDL_LEXP);
            mutations.add(Mutation.CDL_REXP);
        } else {
            mutations.add(Mutation.AORB_PLUS);
        }


        return mutations;
    }

    private static Set<Mutation> targetLExpTimesRExp(BinaryExpression target) {
        Set<Mutation> mutations = new HashSet<>();

        mutations.add(Mutation.AORB_DIVIDE);

        if (operatorEnabled("ODL")) {
            mutations.add(Mutation.ODL_LEXP);
            mutations.add(Mutation.ODL_REXP);
        } else if (operatorEnabled("VDL") || operatorEnabled("CDL")) {
            mutations.add(Mutation.VDL_LEXP);
            mutations.add(Mutation.VDL_REXP);
            mutations.add(Mutation.CDL_LEXP);
            mutations.add(Mutation.CDL_REXP);
        }

        return mutations;
    }

    private static Set<Mutation> targetLExpDivideRExp(BinaryExpression target) {
        Set<Mutation> mutations = new HashSet<>();

        mutations.add(Mutation.AORB_MOD);
        mutations.add(Mutation.AORB_TIMES);

        if (operatorEnabled("ODL")) {
                        mutations.add(Mutation.ODL_REXP);
        } else if (operatorEnabled("VDL") || operatorEnabled("CDL")) {
            mutations.add(Mutation.VDL_REXP);
            mutations.add(Mutation.CDL_REXP);
        }


        return mutations;
    }

    private static Set<Mutation> targetLExpModRExp(BinaryExpression target) {
        Set<Mutation> mutations = new HashSet<>();

        mutations.add(Mutation.AORB_PLUS);
        mutations.add(Mutation.AORB_MINUS);
        mutations.add(Mutation.AORB_DIVIDE);

        if (operatorEnabled("ODL")) {
            mutations.add(Mutation.ODL_LEXP);
        } else if (operatorEnabled("VDL") || operatorEnabled("CDL")) {
            mutations.add(Mutation.VDL_LEXP);
            mutations.add(Mutation.CDL_LEXP);
        }

        return mutations;
    }

    private static Set<Mutation> targetLExpLogicalAndRExp(BinaryExpression target) {
        Set<Mutation> mutations = new HashSet<>(getDeletionMutations());

        mutations.add(Mutation.COR_FALSE);
        mutations.add(Mutation.COR_EQUAL);

        return mutations;
    }

    private static Set<Mutation> targetLExpLogicalOrRExp(BinaryExpression target) {
        Set<Mutation> mutations = new HashSet<>(getDeletionMutations());

        mutations.add(Mutation.COR_TRUE);
        mutations.add(Mutation.COR_NOTEQUAL);

        return mutations;
    }

    private static Set<Mutation> targetLExpXorRExp(BinaryExpression target) {
        Set<Mutation> mutations = new HashSet<>();

        mutations.add(Mutation.COR_OR);
        mutations.add(Mutation.COR_FALSE);

        return mutations;
    }

    private static Set<Mutation> targetLExpGreaterRExp(BinaryExpression target) {
        Set<Mutation> mutations = new HashSet<>();

        mutations.add(Mutation.ROR_FALSE);
        mutations.add(Mutation.ROR_NOTEQUAL);
        mutations.add(Mutation.ROR_GREATEREQUAL);

        return mutations;
    }

    private static Set<Mutation> targetLExpGreaterEqualRExp(BinaryExpression target) {
        Set<Mutation> mutations = new HashSet<>();

        mutations.add(Mutation.ROR_TRUE);
        mutations.add(Mutation.ROR_EQUAL);
        mutations.add(Mutation.ROR_GREATER);

        return mutations;
    }

    private static Set<Mutation> targetLExpLessRExp(BinaryExpression target) {
        Set<Mutation> mutations = new HashSet<>();

        mutations.add(Mutation.ROR_FALSE);
        mutations.add(Mutation.ROR_NOTEQUAL);
        mutations.add(Mutation.ROR_LESSEQUAL);

        return mutations;
    }

    private static Set<Mutation> targetLExpLessEqualRExp(BinaryExpression target) {
        Set<Mutation> mutations = new HashSet<>();

        mutations.add(Mutation.ROR_TRUE);
        mutations.add(Mutation.ROR_EQUAL);
        mutations.add(Mutation.ROR_LESS);

        return mutations;
    }

    private static Set<Mutation> targetLExpEqualRExp(BinaryExpression target) {
        Set<Mutation> mutations = new HashSet<>();

        mutations.add(Mutation.ROR_FALSE);
        mutations.add(Mutation.ROR_GREATEREQUAL);
        mutations.add(Mutation.ROR_LESSEQUAL);

        return mutations;
    }

    private static Set<Mutation> targetLExpNotEqualRExp(BinaryExpression target) {
        Set<Mutation> mutations = new HashSet<>();

        mutations.add(Mutation.ROR_TRUE);
        mutations.add(Mutation.ROR_GREATER);
        mutations.add(Mutation.ROR_LESS);

        return mutations;
    }

    private static Set<Mutation> targetLExpEqualRExpBoolean(BinaryExpression target) {
        return new HashSet<>(getDeletionMutations());
    }

    private static Set<Mutation> targetLExpNotEqualRExpBoolean(BinaryExpression target) {
        return new HashSet<>(getDeletionMutations());
    }

    private static Set<Mutation> targetLExpEqualObjectRExp(BinaryExpression target) {
        Set<Mutation> mutations = new HashSet<>();

        mutations.add(Mutation.ROR_NOTEQUAL);

        return mutations;
    }

    private static Set<Mutation> targetLExpNotEqualObjectRExp(BinaryExpression target) {
        Set<Mutation> mutations = new HashSet<>();

        mutations.add(Mutation.ROR_EQUAL);

        return mutations;
    }

    private static Set<Mutation> targetLExpBitXorRExp(BinaryExpression target) {
        Set<Mutation> mutations = new HashSet<>();

        mutations.add(Mutation.LOR_BITOR);

        return mutations;
    }

    private static Set<Mutation> targetLExpBitAndRExp(BinaryExpression target) {
        return new HashSet<>(getDeletionMutations());
    }

    private static Set<Mutation> targetLExpBitOrRExp(BinaryExpression target) {
        Set<Mutation> mutations = new HashSet<>(getDeletionMutations());

        mutations.add(Mutation.LOR_BITXOR);

        return mutations;
    }

    private static Set<Mutation> getDeletionMutations() {
        Set<Mutation> mutations = new HashSet<>();

        if (operatorEnabled("ODL")) {
            mutations.add(Mutation.ODL_LEXP);
            mutations.add(Mutation.ODL_REXP);
        } else if (operatorEnabled("VDL") || operatorEnabled("CDL")) {
            mutations.add(Mutation.VDL_LEXP);
            mutations.add(Mutation.VDL_REXP);
            mutations.add(Mutation.CDL_LEXP);
            mutations.add(Mutation.CDL_REXP);
        }

        return mutations;
    }

    private static void setMutationsTo(Expression expression, Set<Mutation> mutations) {
        getCurrentTargets().put(expression.getObjectID(), mutations);
        if (expression instanceof UnaryExpression) {
            expression = ((UnaryExpression) expression).getExpression();
            getCurrentTargets().put(expression.getObjectID(), mutations);
        }
    }

    public static void setCurrentFile(File file) {
        currentFile = file;
    }

    private static boolean hasArithmeticType(BinaryExpression expression) {
        return hasArithmeticType(expression.getLeft())
                && hasArithmeticType(expression.getRight());
    }

    private static boolean hasArithmeticType(Expression expression) {
        try {
            if (mutator instanceof Arithmetic_OP) {
                return ((Arithmetic_OP) mutator).isArithmeticType(expression);
            }
        } catch(ParseTreeException e) {
            e.printStackTrace();
        }

        return false;
    }

    private static boolean hasBooleanType(BinaryExpression expression) {
        return hasBooleanType(expression.getLeft())
                && hasBooleanType(expression.getRight());
    }

    private static boolean hasBooleanType(Expression expression) {
        try {
            return mutator.getType(expression) == OJSystem.BOOLEAN;
        } catch (ParseTreeException e) {
            e.printStackTrace();
        }

        return false;
    }

    public static boolean isNewMutationsEnabled() {
        return newMutationsEnabled;
    }

    public static void setEnabledRules(boolean enable) {
        enabled = enable;
    }

    public static void setNewMutationsEnabled(boolean enable) {
        newMutationsEnabled = enable;
    }

}
