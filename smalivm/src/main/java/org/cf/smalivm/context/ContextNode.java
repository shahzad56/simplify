package org.cf.smalivm.context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cf.smalivm.opcode.Op;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContextNode {

    private static Logger log = LoggerFactory.getLogger(ContextNode.class.getSimpleName());

    private final static String DOT = "[^a-zA-Z\200-\377_0-9\\s\\p{Punct}]";

    private final List<ContextNode> children;
    private final Op op;
    private final Map<String, ClassContext> classNameToClassContext;
    private MethodContext mctx;
    private ContextNode parent;

    public ContextNode(ContextNode other) {
        this(other.op);

        if (other.getMethodContext() != null) {
            mctx = new MethodContext(other.getMethodContext());
        }

        for (String className : other.getClassContextNames()) {
            ClassContext cctx = other.getClassContext(className);
            setClassContext(className, cctx);
        }
    }

    public ClassContext getClassContext(String className) {
        return classNameToClassContext.get(className);
    }

    public void setClassContext(String className, ClassContext cctx) {
        classNameToClassContext.put(className, cctx);
    }

    public Set<String> getClassContextNames() {
        return classNameToClassContext.keySet();
    }

    public ContextNode(Op op) {
        this.op = op;

        // Most nodes will only have one child.
        children = new ArrayList<ContextNode>(1);
        classNameToClassContext = new HashMap<String, ClassContext>();
    }

    public void addChild(ContextNode child) {
        child.setParent(this);
        children.add(child);
    }

    public void removeChild(ContextNode child) {
        children.remove(child);
    }

    public void replaceChild(ContextNode oldChild, ContextNode newChild) {
        int index = children.indexOf(oldChild);
        children.remove(index);
        children.add(index, newChild);
    }

    public int[] execute() {
        log.debug("HANDLING @" + op.getAddress() + ": " + op + "\nContext before: " + mctx);
        int[] result = op.execute(mctx);
        log.debug("Context after: " + mctx);

        return result;
    }

    public int getAddress() {
        return op.getAddress();
    }

    public List<ContextNode> getChildren() {
        return children;
    }

    public MethodContext getMethodContext() {
        return mctx;
    }

    public Op getOpHandler() {
        return op;
    }

    public ContextNode getParent() {
        return parent;
    }

    public String toGraph() {
        List<ContextNode> visitedNodes = new ArrayList<ContextNode>();
        StringBuilder sb = new StringBuilder("digraph {\n");
        getGraph(sb, visitedNodes);
        sb.append("}");

        return sb.toString();
    }

    @Override
    public String toString() {
        return op.toString();
    }

    private void getGraph(StringBuilder sb, List<ContextNode> visitedNodes) {
        if (visitedNodes.contains(this)) {
            return;
        }
        visitedNodes.add(this);

        for (ContextNode child : getChildren()) {
            String op = toString().replaceAll(DOT, "?").replace("\"", "\\\"");
            String ctx = getMethodContext().toString().replaceAll(DOT, "?").replace("\"", "\\\"").trim();
            sb.append("\"").append(getAddress()).append("\n").append(op).append("\n").append(ctx).append("\"");

            sb.append(" -> ");

            op = toString().replaceAll(DOT, "?").replace("\"", "\\\"");
            ctx = getMethodContext().toString().replaceAll(DOT, "?").replace("\"", "\\\"").trim();
            sb.append("\"").append(getAddress()).append("\n").append(op).append("\n").append(ctx).append("\"");
            sb.append("\n");
            op = null;
            ctx = null;

            child.getGraph(sb, visitedNodes);
        }
    }

    public void setParent(ContextNode parent) {
        // All nodes will have [0,1] parents since a node represents both an instruction and a context, or vm state.
        // Each execution of an instruction will have a new state.
        this.parent = parent;
    }

    public void setMethodContext(MethodContext ctx) {
        this.mctx = ctx;
    }

}
