# OpenAI Text Processor


## Перед запуском

- Перед запуском необходимо настроить OpenAi совместимый бэк, к примеру:
  LM Studio с включенным сервером и загруженной моделью qwen3-8b и прописать адрес в конфиге
   ```yml
   spring:
     ai:
       openai:
         base-url: http://localhost:1234
   ```

## Requirements

- Java 17+
- Maven
- OpenAi совместимый сервер

## Пример работы:
User
> "Tell me all pets names from all owners, group them by owners"

Ai
> Here are all the pet names grouped by their owners:
>    
>  **Ivan**
>  - Copka
>  - Mike
>  
>  **Maria**
>  - Lapka

User
> "Add new Pet to the database under name John, pet name is Firefly"

Ai
> The pet "Firefly" has been successfully added to the database under the owner "John". Let me know if you'd like to add another pet or need further assistance!

User
> "List pets of John"

Ai
> Here are the pets belonging to John:
> 
> - **Firefly**

--------

User
> "Меня зовут Иван"

Ai
> Здравствуйте, Иван! Рад видеть вас в Spring Petclinic. Как я могу вам помочь? Пожалуйста, уточните ваш запрос.

User
> "У меня есть новое домашнее животное, зовут Барсик"

Ai
> Ваш питомец Барсик успешно зарегистрирован в нашей клинике и привязан к вашему имени Иван. Если у вас есть другие вопросы или нужна помощь, пожалуйста, дайте знать! 🐾



User
>"Tell me all pets names from all owners, group them by owners"

Ai
> Вот список питомцев, сгруппированный по владельцам:
> 
> **Ivan**
> - Copka
> - Mike
> 
> **Иван**
> - Барсик
> 
> **Maria**
> - Lapka
> 
> Если у вас есть дополнительные вопросы или нужна помощь, пожалуйста, дайте знать! 🐾