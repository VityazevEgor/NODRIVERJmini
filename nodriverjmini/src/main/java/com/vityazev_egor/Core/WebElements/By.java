package com.vityazev_egor.Core.WebElements;

public abstract class By {
    public abstract String getJavaScript();
    public abstract String getMultiJavaScript();

    public String replaceQuotes(String input){
        return input.replace("'", "\"");
    }

    public static By id(String id) {
        return new ById(id);
    }

    public static By cssSelector(String selector) {
        return new ByCssSelector(selector);
    }

    public static By className(String className) {
        return new ByClassName(className);
    }

    public static By name(String name) {
        return new ByName(name);
    }

    private static class ById extends By {
        private final String elementJs;

        public ById(String id) {
            this.elementJs = String.format("document.getElementById('%s')", id);
        }

        @Override
        public String getJavaScript() {
            return elementJs;
        }

        @Override
        public String getMultiJavaScript() {
            throw new UnsupportedOperationException("Unimplemented method 'getMultiJavaScript' cuz there can not be multiple elements with the same id");
        }
    }

    private static class ByCssSelector extends By {
        private final String query;

        public ByCssSelector(String selector) {
            this.query = selector;
        }

        @Override
        public String getJavaScript() {
            return String.format("document.querySelector('%s')", replaceQuotes(query));
        }

        @Override
        public String getMultiJavaScript() {
            return String.format("document.querySelectorAll('%s')", replaceQuotes(query));
        }
    }

    private static class ByClassName extends By {
        private final String className;

        public ByClassName(String className) {
            this.className = className;
        }

        @Override
        public String getJavaScript() {
            return String.format("document.getElementsByClassName('%s')[0]", replaceQuotes(className));
        }

        @Override
        public String getMultiJavaScript() {
            return String.format("document.getElementsByClassName('%s')", replaceQuotes(className));
        }
    }

    private static class ByName extends By {
        private final String name;

        public ByName(String name) {
            this.name = name;
        }

        @Override
        public String getJavaScript() {
            return String.format("document.getElementsByName('%s')[0]", replaceQuotes(name));
        }

        @Override
        public String getMultiJavaScript() {
            return String.format("document.getElementsByName('%s')", replaceQuotes(name));
        }
    }
}
