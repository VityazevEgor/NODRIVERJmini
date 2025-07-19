package com.vityazev_egor;

import java.io.IOException;
import java.util.Scanner;
import javax.imageio.*;

import com.vityazev_egor.Core.WaitTask;
import com.vityazev_egor.Core.WebElements.By;

import java.nio.file.Path;

public class Application {

    public static void main(String[] args) throws IOException, InterruptedException {
        NoDriver driver = new NoDriver();
        driver.getXdo().calibrate();
        driver.getNavigation().loadUrlAndWait("https://ya.ru", 10);
        var input = driver.findElement(By.id("text"));
        input.getPosition().ifPresent(point -> driver.getXdo().click(point.getX(), point.getY()));
        waitEnter();
        driver.exit();
    }

    public static void waitEnter(){
        var sc = new Scanner(System.in);
        sc.nextLine();
    }
}
