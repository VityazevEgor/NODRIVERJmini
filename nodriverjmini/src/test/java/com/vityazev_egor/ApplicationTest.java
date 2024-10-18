package com.vityazev_egor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

class ApplicationTest {

    @Test
    void shouldAnswerWithTrue() {
        assertTrue(true);
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
