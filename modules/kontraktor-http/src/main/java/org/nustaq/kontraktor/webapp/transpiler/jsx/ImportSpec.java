package org.nustaq.kontraktor.webapp.transpiler.jsx;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ImportSpec {
    List<String> components = new ArrayList();
    List<String> aliases = new ArrayList();;
    String component;
    String alias;
    private String from;
    File requiredin;
    boolean isRequire;

    public File getRequiredin() {
        return requiredin;
    }

    public List<String> getComponents() {
        return components;
    }

    public List<String> getAliases() {
        return aliases;
    }

    public String getComponent() {
        return component;
    }

    public String getAlias() {
        return alias;
    }

    public String getFrom() {
        return from;
    }

    public ImportSpec components(List<String> components) {
        this.components = components;
        return this;
    }

    public boolean isRequire() {
        return isRequire;
    }

    public ImportSpec aliases(List<String> aliases) {
        this.aliases = aliases;
        return this;
    }

    public ImportSpec component(String component) {
        this.component = component;
        return this;
    }

    public ImportSpec alias(String alias) {
        this.alias = alias;
        return this;
    }

    public ImportSpec from(String from) {
        this.from = from;
        return this;
    }

    /**
     * @return wether this is a pure "import 'directfile.xy'" statement (outside node modules)
     */
    public boolean isPureImport() {
        return (component == null || component.isEmpty()) && (aliases == null || aliases.isEmpty()) && from != null && ! isRequire();
    }

    @Override
    public String toString() {
        return "ImportSpec{" +
            "components=" + components +
            ", aliases=" + aliases +
            ", component='" + component + '\'' +
            ", alias='" + alias + '\'' +
            ", from='" + from + '\'' +
            ", requiredin=" +
            requiredin.getParentFile().getParentFile().getParentFile().getName() + "/" +
            requiredin.getParentFile().getParentFile().getName() + "/" +
            requiredin.getParentFile().getName()+"/"+
            requiredin.getName()+
            ", isRequire=" + isRequire +
            '}';
    }

    public ImportSpec requiredin(File requiredin) {
        this.requiredin = requiredin;
        return this;
    }
}
