function captureElementAsBase64(elementId) {
    // Находим элемент по указанному ID
    const element = document.getElementById(elementId);
    if (!element) {
        console.error(`Element with ID "${elementId}" not found.`);
        return "null";
    }

    return html2canvas(element, {
        useCORS: true, // Учет кросс-доменных ресурсов, если они поддерживают CORS
    }).then(canvas => {
        // Преобразуем холст в строку Base64 и удаляем префикс
        const base64Image = canvas.toDataURL("image/png").replace(/^data:image\/png;base64,/, "");

        // Находим блок с id="base64image" или создаём его, если он отсутствует
        let base64Container = document.getElementById("base64image");
        if (!base64Container) {
            base64Container = document.createElement("div");
            base64Container.id = "base64image";
            base64Container.style.display= "none"; // Убираем отображение блока
            document.body.appendChild(base64Container); // Добавляем блок в конец документа
        }

        // Вставляем Base64 строку внутрь блока
        base64Container.textContent = base64Image;
        console.log("Base64 Image added to #base64image:", base64Image);
        return base64Image; // Возвращаем строку
    }).catch(error => {
        console.error("Failed to capture the element:", error);
        return "null";
    });
}

captureElementAsBase64("toCapture");
