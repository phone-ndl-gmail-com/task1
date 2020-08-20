package com.ndl;

import java.io.File;
import java.io.FileNotFoundException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Main {

// регулярки для проверки на корректность ввода
    final static String NUMBER  = "^-?\\d+(\\.\\d+)?$";
    final static String CODE    = "^[A-Z]{3}$";
    final static String PAYMENT = "^[A-Z]{3} -?\\d+(\\.\\d+)?$";

// проверка на корректность ввода денежных сумм
    public static boolean isNotValid(String str, String regex) {
        return !str.trim().matches(regex);
    }

// главная процедура
    public static void main(String[] args) throws FileNotFoundException {

        // создаем map-счетчик
        PaymentCounter counter = new PaymentCounter();
        String inputLine;       // входная строка для приема платежа
        String[] inputArray;    // массив из входной строки

        // если есть параметр - имя файла, обработаем его
        if(args.length != 0) {

            // входной поток из файла
            Scanner sf = new Scanner(new File(args[0]));
            while(sf.hasNext()){
                inputLine = sf.nextLine();

                // проверяем корректность строки - ошибки пропускаем
                if(isNotValid(inputLine, PAYMENT))
                    continue;

                // строку в массив
                inputArray = inputLine.trim().split(" ");

                // добавим сумму в счетчик
                counter.add(inputArray[0].trim(), new BigDecimal(inputArray[1]));
            }
            sf.close();
        }

        // стартует поток вывода (раз в минуту)
        OutputResult outputResult = new OutputResult(counter);
        outputResult.start();

        // входной поток (консоль)
        Scanner in = new Scanner(System.in);
        do {
            System.out.print("Input payment ([currency code] [amount]): ");
            inputLine = in.nextLine();

        // выход по quit
            if(inputLine.toLowerCase().trim().equals("quit")) {
                break;
            }

        // если просто нажали ENTER
            if(inputLine.trim().isEmpty()) {
                System.out.println("Enter something!");
                continue;
            }

        // код валют должен состоять из 3 символов
            inputArray = inputLine.split(" ");
            if(isNotValid(inputArray[0], CODE)) {
                System.out.println("Invalid input! (Example of currency code: USD, RUB, ...)");
                continue;
            }

        // если введен только 1 параметр
            if(inputArray.length<2) {
                System.out.println("Enter amount!");
                continue;
            }

        // проверка ввода денежной суммы
            if(isNotValid(inputArray[1], NUMBER)) {
                System.out.println("Invalid input! The second value must be numeric!");
                continue;
            }

        // добавим платеж в сводную таблицу
            counter.add(inputArray[0].trim(), new BigDecimal(inputArray[1]));
        } while (true);

        // при выходе из цикла закроем поток вывода
        in.close();

        // и прерываем поток(thead) вывода
        outputResult.interrupt();
    }
}

// класс итоговой таблицы
class PaymentCounter extends HashMap<String, BigDecimal> {

// перепишем строковое представление таблицы
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder("\n\n");
        synchronized (this) {
            for (Map.Entry<String, BigDecimal> item : entrySet()) {
            // не выводим валюту с суммой = 0
                if(item.getValue().equals(BigDecimal.ZERO))
                    continue;
                result.append(item.getKey());
                result.append(" ");
                result.append(new DecimalFormat("#0.00").format(item.getValue()));
                result.append("\n");
            }
        }

    // если нет валют с суммами != 0 - не выводим ничего!
        if(result.toString().equals("\n\n"))
            return "";
    // иначе приглашение для ввода
        else
            result.append("Input payment ([currency code] [amount]): ");
        return result.toString();
    }
// метод добавления платежа (подсчет сумм валют)
    public void add(String currencyCode, BigDecimal amount) {
        BigDecimal currencyAmount = get(currencyCode);
        if(currencyAmount==null)
            put(currencyCode, amount);
        else
            put(currencyCode, currencyAmount.add(amount));
    }
}

// класс вывода итоговой таблицы типа Thread (Runnable)
class OutputResult extends Thread {

    boolean stop = false;       // на всякий случай
    PaymentCounter counter;     // таблица (объект из Main)

    OutputResult(PaymentCounter counter) {
        this.counter = counter;     // передаем ссылку на таблицу через конструктор
    }

// что делает второй поток: в цикле -> пауза 1мин. и вывод таблицы
    @Override
    public void run() {
        while (!stop) {
            try {
                TimeUnit.MINUTES.sleep(1); // Задержка в 1 мин.
            } catch (InterruptedException e) {
                System.out.println("outMonitor is stopped!");
                break;
            }
            System.out.print(counter);
        }
    }
}