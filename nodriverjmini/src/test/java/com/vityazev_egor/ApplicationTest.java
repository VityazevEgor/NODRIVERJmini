package com.vityazev_egor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Paths;

class ApplicationTest {

    @Test
    void testCFBypassXDO() throws IOException{
        NoDriver d = new NoDriver("127.0.0.1:2080");
        d.getXdo().calibrate();
        Boolean result = d.getNavigation().loadUrlAndBypassCFXDO("https://dstatlove.ink/hit", 5, 30);
        d.exit();
        assertTrue(result);
    }

    @Test
    void testCFBypassCDP()throws IOException{
        NoDriver d = new NoDriver();
        Boolean result = d.getNavigation().loadUrlAndBypassCFCDP("https://nopecha.com/demo/cloudflare", 10, 30);
        d.exit();
        assertTrue(result);
    }

    @Test
    void testAntiBot() throws IOException{
        NoDriver d = new NoDriver("127.0.0.1:2080");
        Boolean result = d.getNavigation().loadUrlAndWait("https://bot.sannysoft.com/", 10);
        d.getMisc().captureScreenshot(Paths.get("antibot.png"));
        d.exit();
        assertTrue(result);
    }

    @Test
    void testViewPort() throws IOException{
        NoDriver d = new NoDriver();
        var dm = d.getViewPortSize();
        System.out.println("Dimesion viewport= "+dm.get().getWidth() + ":"+dm.get().getHeight());
        d.exit();
        assertTrue(dm.isPresent());
    }

    @Test
    void testLoadAndWait() throws IOException, InterruptedException{
        NoDriver d = new NoDriver();
        Boolean result = d.getNavigation().loadUrlAndWait("https://bing.com", 10);
        Boolean result2 = d.getNavigation().loadUrlAndWait("https://ya.ru", 10);
        d.exit();
        assertTrue(result);
        assertTrue(result2);
    }

    @Test
    void testJs() throws IOException{
        NoDriver d = new NoDriver();
        var title = d.getTitle();
        d.exit();
        assertTrue(title.isPresent());
        System.out.println(title.get());
    }
}
