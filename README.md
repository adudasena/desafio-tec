# Desafio Técnico 

Implementação do desafio técnico utilizando Java.

## O que o programa faz

1. Consome a API pública do IBGE para obter dados de municípios.
2. Lê o arquivo `input.csv` com municípios e populações.
3. Normaliza nomes e trata pequenos erros de digitação.
4. Enriquecimento com dados do IBGE (UF, região e ID).
5. Gera `resultado.csv`.
6. Calcula estatísticas agregadas.
7. Envia os resultados para a API de correção.

## Tecnologias utilizadas

- Java
- HttpClient
- Gson
- API do IBGE
- Supabase

## Execução

DesafioNasajon.java

## Como rodar o projeto
O projeto foi desenvolvido em Java 17 utilizando apenas bibliotecas nativas e o Gson para manipulação de JSON.
Pré-requisitos
• Java JDK 11 ou superior instalado.
• Biblioteca gson-2.10.1.jar no classpath.
• Arquivo input.csv na raiz da pasta do projeto.
Passo a passo para execução:

javac -cp ".;gson-2.10.1.jar" DesafioNasajon.java



