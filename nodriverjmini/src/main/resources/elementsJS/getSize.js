function getElementSize() {
    var element = REPLACE_ME;
    if (element) {
        var rect = element.getBoundingClientRect();
        var position = {
            x: Math.round(rect.width),
            y: Math.round(rect.height)
        };
        return JSON.stringify(position);
    } else {
        return JSON.stringify({ error: "Element not found" });
    }
}
getElementSize();