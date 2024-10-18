function getElementPosition() {
    var element = REPLACE_ME;
    if (element) {
        var rect = element.getBoundingClientRect();
        var position = {
            x: Math.round(rect.left + rect.width / 2),
            y: Math.round(rect.top + rect.height / 2)
        };
        return JSON.stringify(position);
    } else {
        return JSON.stringify({ error: "Element not found" });
    }
}
getElementPosition();