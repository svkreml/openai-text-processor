# OpenAI Text Processor

http://localhost:8080/index.html

## Описание проекта
OpenAI Text Processor — это Spring Boot приложение, интегрирующее AI-модели для работы с данными . Проект демонстрирует взаимодействие с моделью Qwq-32b через совместимый бэкенд (например, LM Studio) .

## Перед запуском
1. Настройте OpenAI-совместимый сервер (например, LM Studio):
   - Запустите сервер и загрузите модель `qwq-32b`.
   - Укажите URL в конфигурации:
     ```yml
     spring:
       ai:
         openai:
           base-url: http://127.0.0.1:1234
     ```

## Настройки конфигурации
| Свойство                                             | Значение                | Описание                                                                                       |
|------------------------------------------------------|-------------------------|------------------------------------------------------------------------------------------------|
| `spring.ai.openai.model`                             | `qwq-32b`               | Используемая AI-модель                                                                         |
| `spring.ai.openai.base-url`                          | `http://127.0.0.1:1234` | Базовый URL для API-совместимой с OpenAI точки доступа.                                        |
| `spring.ai.openai.no-think`                          | `false`                 | Отключает процесс \"мышления\"; работает, только если модель это поддерживает. *(Опционально)* |
| `spring.ai.openai.api-key`                           | `NOT_REQUIRED`          | Указывает, что ключ API для аутентификации не требуется.                                       |
| `logging.org.springframework.ai.chat.client.advisor` | `INFO`                  | Уровень логирования для взаимодействия с AI-моделью.                                           | 
| `file.base.dir`                                      | `./`                    | Базовая директория для хранения файлов проекта                                                 |

## Требования
- Java 21
- Maven
- OpenAI-совместимый сервер (например, LM Studio)

## Инструкции для запуска llama.cpp

Для запуска сервера llama.cpp выполните следующую команду:

```bash
env GGML_VK_VISIBLE_DEVICES="" env ROCR_VISIBLE_DEVICES="0,1,2,3" ./llama-server --host 0.0.0.0 --ctx-size 16000 --numa numactl --gpu-layers 999 --batch-size 512 --jinja -m /home/svkreml/llama/models/qwq-32b-q4_k_m.gguf
```

ROCR_VISIBLE_DEVICES - ROCM
GGML_VK_VISIBLE_DEVICES - VULKAN