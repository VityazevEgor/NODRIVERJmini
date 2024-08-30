package com.vityazev_egor.Core.WebElements;

public abstract class By {
    public abstract String getJavaScript();

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
    }

    private static class ByCssSelector extends By {
        private final String elementJs;

        public ByCssSelector(String selector) {
            this.elementJs = String.format("document.querySelector('%s')", selector);
        }

        @Override
        public String getJavaScript() {
            return elementJs;
        }
    }

    private static class ByClassName extends By {
        private final String elementJs;

        public ByClassName(String className) {
            this.elementJs = String.format("document.getElementsByClassName('%s')[0]", className);
        }

        @Override
        public String getJavaScript() {
            return elementJs;
        }
    }

    private static class ByName extends By {
        private final String elementJs;

        public ByName(String name) {
            this.elementJs = String.format("document.getElementsByName('%s')[0]", name);
        }

        @Override
        public String getJavaScript() {
            return elementJs;
        }
    }
}
