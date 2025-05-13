package ru.petsinbloom.jFerzl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    static final AppConfig config = AppConfig.INSTANCE;

    static boolean IsStateMachine;

    public static void main(String[] args) {
        logger.info("Start");

        int threadCount = Runtime.getRuntime().availableProcessors();
        System.out.println("Доступно процессоров: "+threadCount);

        FerzlWorkflow wf = new FerzlWorkflow(System.out::println);
        // FerzlStateMachine sm = new FerzlStateMachine(System.out::println);

        if (config.isStateMachine()) {
            /// Перейти к запуску State Machine
            // wf.runFrom(wf.loadLastState());
            //sm.runFrom(sm.loadLastState());
        }else{

            /// Оставить линейную pipeline логику
             wf.runWorkFlow();
        }

        logger.info("End");
    }
}
