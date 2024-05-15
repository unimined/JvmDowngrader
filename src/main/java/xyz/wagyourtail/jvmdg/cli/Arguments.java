package xyz.wagyourtail.jvmdg.cli;

import java.util.*;

public class Arguments {
    private final Map<Set<String>, Arguments> children = new LinkedHashMap<>();
    private final String shortDesc;
    private final String name;
    private final Set<String> altNames;
    private final String[] valueNames;

    public Arguments(String name, String shortDesc, String[] altNames, String[] valueNames) {
        this.name = name;
        this.shortDesc = shortDesc;
        Set<String> names = new LinkedHashSet<>();
        names.add(name.toLowerCase(Locale.ROOT));
        if (altNames != null) {
            names.addAll(Arrays.asList(altNames));
        }
        this.altNames = Collections.unmodifiableSet(names);
        if (valueNames == null) {
            valueNames = new String[0];
        }
        this.valueNames = valueNames;
    }

    public Arguments addChildren(Arguments... children) {
        for (Arguments child : children) {
            for (Set<String> strings : this.children.keySet()) {
                if (!Collections.disjoint(strings, child.altNames)) {
                    throw new IllegalArgumentException("Duplicate argument: " + strings);
                }
            }
            this.children.put(child.altNames, child);
        }
        return this;
    }

    public String help() {
        StringBuilder ret = new StringBuilder();
        for (Arguments child : children.values()) {
            ret.append("\n").append(indent(child.helpIntl()));
        }
        return ret.toString();
    }

    public String helpIntl() {
        StringBuilder ret = new StringBuilder();
        ret.append(name).append(", ");
        for (String altName : altNames) {
            if (name.toLowerCase(Locale.ROOT).equals(altName)) {
                continue;
            }
            ret.append(altName).append(", ");
        }

        ret.setLength(ret.length() - 2);
        if (valueNames.length > 0) {
            ret.append("  <");
            for (String valueName : valueNames) {
                ret.append(valueName).append("> <");
            }
            ret.setLength(ret.length() - 3);
            ret.append(">");
        }
        if (shortDesc != null && !shortDesc.isEmpty()) {
            ret.append(" : ").append(shortDesc);
        }
        if (!children.isEmpty()) {
            ret.append(indent(help()));
        }
        return ret.toString();
    }

    public String indent(String s) {
        return s.replaceAll("\n", "\n  ");
    }

    public Map<String, List<String[]>> read(List<String> args, boolean allowExtra) {
        Map<String, List<String[]>> ret = new LinkedHashMap<>();
        outer:
        while (!args.isEmpty()) {
            String arg = args.remove(0).toLowerCase(Locale.ROOT);
            for (Set<String> strings : children.keySet()) {
                if (strings.contains(arg)) {
                    List<String> values = new ArrayList<>();
                    Arguments child = children.get(strings);
                    for (int i = 0; i < child.valueNames.length; i++) {
                        if (args.isEmpty()) {
                            throw new IllegalArgumentException("Missing value for " + arg);
                        }
                        values.add(args.remove(0));
                    }
                    if (!ret.containsKey(child.name)) {
                        ret.put(child.name, new ArrayList<String[]>());
                    }
                    ret.get(child.name).add(values.toArray(new String[0]));
                    for (Map.Entry<String, List<String[]>> entry : child.read(args, true).entrySet()) {
                        if (ret.containsKey(child.name + ":" + entry.getKey())) {
                            ret.get(child.name + ":" + entry.getKey()).addAll(entry.getValue());
                        } else {
                            ret.put(child.name + ":" + entry.getKey(), entry.getValue());
                        }
                    }
                    continue outer;
                }
            }
            if (allowExtra) {
                args.add(0, arg);
                break;
            } else {
                throw new IllegalArgumentException("Unknown argument: " + arg);
            }
        }
        return ret;
    }

    public static Map<String, List<String[]>> subCommandArgs(String prefix, Map<String, List<String[]>> args) {
        Map<String, List<String[]>> ret = new HashMap<>();
        for (Map.Entry<String, List<String[]>> entry : args.entrySet()) {
            if (entry.getKey().startsWith(prefix + ":")) {
                ret.put(entry.getKey().substring(prefix.length() + 1), entry.getValue());
            }
        }
        return ret;
    }

}
