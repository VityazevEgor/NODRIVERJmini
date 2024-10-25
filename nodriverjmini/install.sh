#!/bin/bash

mvn clean compile assembly:single
if [ $? -ne 0 ]; then
  echo "Ошибка при выполнении mvn clean compile assembly:single"
  exit 1
fi

cd target || { echo "Не удалось перейти в папку target"; exit 1; }
jar_file=$(find . -name "*.jar" -print -quit)

if [ -z "$jar_file" ]; then
  echo "Не найден .jar файл в папке target"
  exit 1
fi

mvn install:install-file -Dfile="$jar_file" \
  -DgroupId=com.vityazev_egor \
  -DartifactId=nodriverjmini \
  -Dversion=1.0 \
  -Dpackaging=jar \
  -Dname=nodriverjmini

if [ $? -eq 0 ]; then
  echo "Успешно установлен $jar_file в локальный репозиторий"
else
  echo "Ошибка при установке .jar файла в локальный репозиторий"
  exit 1
fi