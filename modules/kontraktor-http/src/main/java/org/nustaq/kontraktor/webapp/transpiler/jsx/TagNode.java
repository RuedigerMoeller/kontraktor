package org.nustaq.kontraktor.webapp.transpiler.jsx;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public class TagNode extends TokenNode {

    protected StringBuilder tagName = new StringBuilder(100);
    List<AttributeNode> attributes = new ArrayList(); // attrs if this is tag

    public void addTagName(char c) {
        if ( tagName == null )
            tagName = new StringBuilder(10);
        if ( c > 0)
            tagName.append(c);
    }

    public boolean isReactComponent() {
        return tagName != null && tagName.length() > 0 && Character.isUpperCase(tagName.charAt(0));
    }

    public StringBuilder getTagName() {
        return tagName;
    }

    public List<AttributeNode> getAttributes() {
        return attributes;
    }

    public void addAttribute(AttributeNode currentAttribute) {
        attributes.add(currentAttribute);
    }

    public void dump(PrintStream p, String indent) {
        p.println(indent+"_"+tagName+(children.size()==0?"/_":"_"));
        for (int i = 0; i < attributes.size(); i++) {
            AttributeNode attributeEntry = attributes.get(i);
            attributeEntry.dump(p,indent+"  ");
        }
        for (int i = 0; i < children.size(); i++) {
            TokenNode tokenNode = children.get(i);
            tokenNode.dump(p,indent+"  ");
        }
    }

}
