function loadScript(src) {
    return new Promise((resolve, reject) => {
        const script = document.createElement("script");
        script.src = src;
        script.onload = resolve;
        script.onerror = reject;
        document.head.appendChild(script);
    });
}

function captureElementAsBase64() {
    // Загружаем библиотеку html2canvas
    loadScript("https://cdnjs.cloudflare.com/ajax/libs/html2canvas/1.4.1/html2canvas.min.js")
        .then(() => {
            // Находим элемент по указанному ID
            const element = REPLACE_ME;
            if (!element) {
                console.error(`Element not found.`);
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
                    base64Container.style.display= "none"; // Убираем блок отображения
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
        })
        .catch((error) => {
            console.error("Failed to load html2canvas:", error);
        });
}

// Вызов функции
captureElementAsBase64();