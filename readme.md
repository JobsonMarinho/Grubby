# Grubby

Grubby é uma linguagem de programação interpretada, desenvolvida em Kotlin utilizando Gradle e JDK 21.  
O foco principal do Grubby é fornecer uma solução eficiente e intuitiva para o desenvolvimento de plugins para servidores de Minecraft.  
Com uma sintaxe clara e fácil de usar, Grubby permite que desenvolvedores criem e personalizem plugins para melhorar a experiência dos jogadores.

## Funcionalidades

- **Sintaxe Simples**: Grubby possui uma sintaxe fácil de aprender e utilizar, ideal para desenvolvimento rápido de plugins.
- **Foco em Minecraft**: Projetada especificamente para facilitar o desenvolvimento de plugins para servidores de Minecraft.
- **Extensível**: A arquitetura do Grubby permite a adição de novos recursos e extensões para atender às necessidades dos desenvolvedores.

## Instalação

Para começar a usar o Grubby, siga os passos abaixo para configurar o ambiente de desenvolvimento:

1. **Clone o repositório**

    ```bash
    git clone https://github.com/JobsonMarinho/grubby.git
    ```

2. **Navegue até o diretório do projeto**

    ```bash
    cd grubby
    ```

3. **Construa o projeto usando Gradle**

    ```bash
    ./gradlew build
    ```

4. **Execute a linguagem**

    ```bash
    ./gradlew run
    ```

## Uso

Para usar o Grubby para desenvolver plugins de Minecraft, crie um arquivo com a extensão `.gr` e escreva seu código. Em seguida, compile o código com o comando Grubby:

```bash
./grubby.jar compile -i "caminho/para/seu/arquivo.gr" -o "caminho/para/salvar/o/arquivo.jar"
```

## Exemplos

Aqui estão alguns exemplos básicos de código em Grubby:

```gr
import other // Importa outro arquivo Grubby

// Declaração de variáveis
var x: number = 10 // Variável
val y: number = 20 // Constante

// Função
fun sum(a: number, b: number): number {
    return a + b
}

// Função com retorno implícito
fun sub(a: number, b: number) {
    return a - b
}

// Função sem retorno
fun printSum(a: number, b: number) {
    println(a + b)
}

// Função sem parâmetros
fun printHello() {
    println 'Hello'
}

println sum(x, y) // 30
```

## Contribuição

Se você deseja contribuir para o projeto Grubby, sinta-se à vontade para enviar pull requests, abrir issues ou sugerir melhorias. Certifique-se de seguir as diretrizes de contribuição descritas no arquivo [CONTRIBUTING](CONTRIBUTING).

## Licença

Este projeto está licenciado sob a Licença [LICENSE](LICENSE) para mais detalhes.
