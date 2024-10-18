function isElementClickable() {
    var element = REPLACE_ME;
    if (!element) {
        return false;
    }
    var style = window.getComputedStyle(element);
    var isVisible = style.display !== 'none' && style.visibility !== 'hidden' && style.opacity > 0;
    var rect = element.getBoundingClientRect();
    var isUnderOtherElement = document.elementFromPoint(rect.left + 1, rect.top + 1) !== element;
    var isEnabled = !element.disabled;
    return isVisible && !isUnderOtherElement && isEnabled;
}
isElementClickable();