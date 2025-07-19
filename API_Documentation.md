# NODRIVERJmini Java Library - API Documentation

## Overview

NODRIVERJmini is a Java library for automated web browser control through Chrome DevTools Protocol (CDP). It provides a lightweight alternative to traditional browser automation tools, offering direct browser control via WebSocket communication. The library supports both Windows and Linux platforms with native system integration.

## Core Classes

### NoDriver

The main entry point for browser automation. Manages Chrome browser instances and provides core functionality for web page interaction.

#### Constructors

```java
// Default constructor - no proxy, non-headless mode
public NoDriver() throws IOException

// Constructor with proxy support
public NoDriver(String socks5Proxy) throws IOException

// Full constructor with proxy and headless options
public NoDriver(String socks5Proxy, Boolean enableHeadless) throws IOException
```

**Parameters:**
- `socks5Proxy` - SOCKS5 proxy configuration (format: "ip:port", can be null)
- `enableHeadless` - Whether to run Chrome in headless mode

#### Core Methods

##### Page Navigation and Control

```java
// Execute JavaScript code in the browser
public void executeJS(String js)

// Execute JavaScript and get return value
public Optional<String> executeJSAndGetResult(String js)

// Get current page HTML
public Optional<String> getHtml()

// Get page title
public Optional<String> getTitle()

// Get current URL
public Optional<String> getCurrentUrl()

// Get viewport dimensions
public Optional<Dimension> getViewPortSize()

// Get page performance timing
public Optional<Double> getCurrentPageTime()
```

##### Element Finding

```java
// Find single element
public WebElement findElement(By by)

// Find multiple elements
public List<WebElement> findElements(By by)
```

##### Browser Management

```java
// Close browser and cleanup resources
public void exit()
```

#### Getter Methods

```java
// Access browser process (for advanced use cases)
public Process getChrome()

// Access extended functionality modules
public XDO getXdo()
public Input getInput()
public Navigation getNavigation()
public Misc getMisc()

// Calibration offsets for XDO clicking
public Integer getCalibrateX()
public Integer getCalibrateY()
```

#### Usage Example

```java
// Basic usage
NoDriver driver = new NoDriver();
driver.getNavigation().loadUrl("https://example.com");
WebElement element = driver.findElement(By.id("myButton"));
driver.getInput().emulateClick(element);
driver.exit();

// With proxy and headless mode
NoDriver driver = new NoDriver("127.0.0.1:1080", true);
// ... automation code
driver.exit();
```

---

### WebElement

Represents a web page element and provides methods for interaction and information retrieval.

#### Constructor

```java
// Created via NoDriver.findElement() or NoDriver.findElements()
// Not intended for direct instantiation
```

#### Element State Methods

```java
// Check if element exists in DOM
public Boolean isExists()

// Check if element is clickable
public Boolean isClickable()

// Get element position (center coordinates)
public Optional<Point> getPosition()

// Get element dimensions
public Optional<Dimension> getSize()
```

#### Content Retrieval Methods

```java
// Get element's inner HTML
public Optional<String> getHTMLContent()

// Get visible text content
public Optional<String> getText()

// Get input field value
public Optional<String> getValue()

// Get attribute value
public Optional<String> getAttribute(String attributeName)
```

#### Interaction Methods

```java
// Set focus on element
public void getFocus()

// Wait for element to appear
public void waitToAppear(Integer timeOutSeconds, Integer delayMilis) throws Exception
```

#### Experimental Methods

```java
// Capture element screenshot (experimental)
public Optional<BufferedImage> testScreenshot()
```

#### Usage Example

```java
WebElement button = driver.findElement(By.id("submitBtn"));

// Check element state
if (button.isExists() && button.isClickable()) {
    // Get element information
    Optional<Point> position = button.getPosition();
    Optional<String> text = button.getText();
    
    // Interact with element
    driver.getInput().emulateClick(button);
}

// Wait for element to appear
WebElement dynamicElement = driver.findElement(By.className("loading"));
dynamicElement.waitToAppear(10, 500); // Wait up to 10 seconds
```

---

### By

Abstract class providing static factory methods for element location strategies.

#### Static Factory Methods

```java
// Find by ID attribute
public static By id(String id)

// Find by CSS selector
public static By cssSelector(String selector)

// Find by class name
public static By className(String className)

// Find by name attribute
public static By name(String name)
```

#### Usage Examples

```java
// By ID
WebElement element = driver.findElement(By.id("username"));

// By CSS selector
WebElement element = driver.findElement(By.cssSelector("div.container > p:first-child"));

// By class name
List<WebElement> elements = driver.findElements(By.className("menu-item"));

// By name attribute
WebElement element = driver.findElement(By.name("email"));
```

---

### Input

Provides methods for simulating user input including mouse movements, clicks, and keyboard input.

#### Constructor

```java
// Access via NoDriver.getInput()
Input input = driver.getInput();
```

#### Mouse Control Methods

```java
// Move mouse to coordinates
public void emulateMouseMove(Integer x, Integer y)

// Click at specific coordinates
public void emulateClick(Integer x, Integer y)
public void emulateClick(Double x, Double y)

// Click on WebElement
public void emulateClick(WebElement element)

// Mouse wheel (experimental)
public void emulateMouseWheel(Integer deltaY, Integer x, Integer y)
```

#### Text Input Methods

```java
// Enter text by simulating keystrokes
public void enterText(WebElement element, String text)

// Insert text directly without keystrokes
public void insertText(WebElement element, String text)
```

#### Usage Examples

```java
Input input = driver.getInput();

// Click at coordinates
input.emulateClick(100, 200);

// Click on element
WebElement button = driver.findElement(By.id("submit"));
input.emulateClick(button);

// Enter text in input field
WebElement textField = driver.findElement(By.name("username"));
input.enterText(textField, "myusername");

// Insert text directly (faster)
input.insertText(textField, "password123");
```

---

### Navigation

Handles page navigation, loading, and Cloudflare bypass functionality.

#### Constructor

```java
// Access via NoDriver.getNavigation()
Navigation nav = driver.getNavigation();
```

#### Basic Navigation Methods

```java
// Load URL
public void loadUrl(String url)

// Navigate back
public void goBack()

// Navigate forward
public void goForward()

// Load URL and wait for completion
public Boolean loadUrlAndWait(String url, Integer timeOutSeconds)

// Wait for current page to load
public Boolean waitFullLoad(Integer timeOutSeconds)
```

#### Cloudflare Bypass Methods

```java
// Generic CF bypass with custom click action
public Boolean loadUrlAndBypassCF(String url, Integer urlLoadTimeOutSeconds, 
    Integer cfBypassTimeOutSeconds, BiConsumer<Integer, Integer> clickAction)

// CF bypass using XDO (works with proxy)
public Boolean loadUrlAndBypassCFXDO(String url, Integer urlLoadTimeOutSeconds, 
    Integer cfBypassTimeOutSeconds)

// CF bypass using CDP (requires clean IP)
public Boolean loadUrlAndBypassCFCDP(String url, Integer urlLoadTimeOutSeconds, 
    Integer cfBypassTimeOutSeconds)
```

#### Usage Examples

```java
Navigation nav = driver.getNavigation();

// Basic navigation
nav.loadUrl("https://example.com");

// Load and wait for completion
boolean success = nav.loadUrlAndWait("https://example.com", 30);

// Cloudflare bypass with XDO
boolean bypassed = nav.loadUrlAndBypassCFXDO("https://protected-site.com", 10, 30);
if (bypassed) {
    System.out.println("Successfully bypassed Cloudflare protection");
}

// Navigation controls
nav.goBack();
nav.goForward();
```

---

### Misc

Provides miscellaneous functionality including screenshot capture, cookie management, and HTML rendering.

#### Constructor

```java
// Access via NoDriver.getMisc()
Misc misc = driver.getMisc();
```

#### Screenshot Methods

```java
// Capture full browser screenshot
public Optional<BufferedImage> captureScreenshot()

// Capture screenshot and save to file
public Optional<BufferedImage> captureScreenshot(Path screenSavePath)

// Convert HTML to image
public Optional<BufferedImage> htmlToImage(Optional<String> optionalHTML)
```

#### Browser Management Methods

```java
// Clear all cookies
public void clearCookies()

// Enable/disable Content Security Policy bypass
public void setBypassCDP(Boolean enabled)
```

#### Usage Examples

```java
Misc misc = driver.getMisc();

// Capture screenshot
Optional<BufferedImage> screenshot = misc.captureScreenshot();
if (screenshot.isPresent()) {
    // Process the image
    BufferedImage image = screenshot.get();
}

// Save screenshot to file
Path outputPath = Paths.get("screenshot.png");
misc.captureScreenshot(outputPath);

// Clear cookies
misc.clearCookies();

// Enable CSP bypass for script injection
misc.setBypassCDP(true);

// Convert HTML to image
Optional<String> html = driver.getHtml();
Optional<BufferedImage> htmlImage = misc.htmlToImage(html);
```

---

### XDO

Provides native system-level mouse control for precise clicking, useful when CDP mouse events don't work (e.g., with certain proxies or protected sites).

#### Constructor

```java
// Access via NoDriver.getXdo()
XDO xdo = driver.getXdo();
```

#### Calibration Methods

```java
// Calibrate browser window position
public Boolean calibrate()
```

#### Click Methods

```java
// Click at browser viewport coordinates
public Boolean click(Integer x, Integer y)
public Boolean click(Double x, Double y)
```

#### Usage Examples

```java
XDO xdo = driver.getXdo();

// Calibrate before first use
boolean calibrated = xdo.calibrate();
if (calibrated) {
    System.out.println("XDO calibration successful");
    
    // Click at viewport coordinates
    boolean clicked = xdo.click(100, 200);
    if (clicked) {
        System.out.println("Click successful");
    }
}

// Use in Cloudflare bypass
driver.getNavigation().loadUrlAndBypassCFXDO("https://protected-site.com", 10, 30);
```

---


### WaitTask (Deprecated)

Abstract class for creating custom wait conditions. **Deprecated** - use `LambdaWaitTask` instead.

---

### LambdaWaitTask

Improved wait task implementation using lambda expressions for more concise condition definitions.

#### Constructor

```java
public LambdaWaitTask(Supplier<Boolean> condition)
```

#### Methods

```java
// Execute wait task
public Boolean execute(Integer timeOutSeconds, Integer delayMillis)
```

#### Usage Examples

```java
// Wait for element to appear
LambdaWaitTask waitTask = new LambdaWaitTask(() -> {
    return driver.findElement(By.id("result")).isExists();
});

boolean appeared = waitTask.execute(10, 500); // Wait up to 10 seconds

// Wait for specific text content
LambdaWaitTask textWait = new LambdaWaitTask(() -> {
    Optional<String> text = driver.findElement(By.id("status")).getText();
    return text.isPresent() && text.get().contains("Complete");
});

boolean textFound = textWait.execute(15, 1000);
```

---

## Recent Improvements

### Version 1.2 Updates

- **Improved Chrome Initialization**: Better error detection and handling during browser startup
- **Enhanced Thread Safety**: Internal WebSocket communication is now fully thread-safe
- **Better User Data Management**: Chrome user data directory is now created in the application directory with proper permissions
- **Performance Optimizations**: Faster message processing and reduced memory usage
- **Modernized Wait Tasks**: Replaced deprecated `WaitTask` with `LambdaWaitTask` for cleaner code

## Platform Support

The library supports both Windows and Linux platforms with platform-specific implementations:

### Linux Support
- Uses `google-chrome` command for browser launching
- Integrates with `xdotool` for native mouse control
- Supports root user execution with `--no-sandbox` flag

### Windows Support
- Automatically detects Chrome installation in common directories
- Uses JNA (Java Native Access) for Windows API integration
- Supports native mouse control via user32.dll

---

## Common Usage Patterns

### Basic Web Automation

```java
NoDriver driver = new NoDriver();
try {
    // Navigate to page
    driver.getNavigation().loadUrlAndWait("https://example.com", 30);
    
    // Find and interact with elements
    WebElement searchBox = driver.findElement(By.name("q"));
    driver.getInput().enterText(searchBox, "search query");
    
    WebElement submitBtn = driver.findElement(By.cssSelector("input[type='submit']"));
    driver.getInput().emulateClick(submitBtn);
    
    // Wait for results
    LambdaWaitTask resultTask = new LambdaWaitTask(() -> 
        driver.findElement(By.id("results")).isExists()
    );
    resultTask.execute(10, 500);
    
} finally {
    driver.exit();
}
```

### Proxy and Headless Usage

```java
NoDriver driver = new NoDriver("127.0.0.1:1080", true); // SOCKS5 proxy, headless
try {
    // Automation code here
} finally {
    driver.exit();
}
```

### Cloudflare Protected Sites

```java
NoDriver driver = new NoDriver("127.0.0.1:1080");
try {
    // Calibrate XDO for precise clicking
    if (driver.getXdo().calibrate()) {
        // Load page with Cloudflare bypass
        boolean success = driver.getNavigation()
            .loadUrlAndBypassCFXDO("https://protected-site.com", 15, 45);
        
        if (success) {
            // Continue with automation
        }
    }
} finally {
    driver.exit();
}
```

### Screenshot and Visual Testing

```java
NoDriver driver = new NoDriver();
try {
    driver.getNavigation().loadUrlAndWait("https://example.com", 30);
    
    // Capture full page screenshot
    Optional<BufferedImage> screenshot = driver.getMisc().captureScreenshot();
    
    // Save to file
    Path outputPath = Paths.get("test_screenshot.png");
    driver.getMisc().captureScreenshot(outputPath);
    
} finally {
    driver.exit();
}
```

---

## Error Handling

Always wrap automation code in try-finally blocks to ensure proper cleanup:

```java
NoDriver driver = null;
try {
    driver = new NoDriver();
    // Automation code
} catch (IOException e) {
    System.err.println("Failed to initialize browser: " + e.getMessage());
} finally {
    if (driver != null) {
        driver.exit();
    }
}
```

## Dependencies

The library requires:
- Java 8 or higher
- Chrome/Chromium browser installed
- For Linux: `xdotool` package for XDO functionality
- Jackson library for JSON processing
- JNA (Java Native Access) for native system integration
- Apache Commons Imaging for image processing

---

## Thread Safety

NODRIVERJmini is **not thread-safe**. Each `NoDriver` instance should be used by a single thread. For concurrent automation, create separate `NoDriver` instances for each thread.