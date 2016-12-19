# Relatório 4 - Verificação e Validação

O objetivo deste relatório é documentar o estado atual do projeto no que respeita à verificação e validação. Numa primeira parte, será feita uma análise relativamente ao grau de testabilidade do projeto, relatando a forma de testar os componentes da aplicação, bem como esta podia ser melhorada. De seguida, são apresentadas algumas estatísticas de teste, relativamente ao número de testes e à cobertura. Por fim, é explicada a forma como resolvemos o *bug* escolhido.

## Testabilidade do *Software* e Comentários

O uso de testes é uma mais valia para garantir a qualidade do projeto, pois permitem determinar a existência de erros importantes. No entanto, os testes não provam que o código esteja isento de falhas.

### Controlabilidade
A controlabilidade, por definição, é o grau que permite controlar o estado do componente a ser testado (CUT - *Component Under Test*).

Analisando os testes do *ownCloud*, verificamos que são bastantes específicos, ou seja, a sua controlabilidade é tanto maior quanto a especificidade do teste.

### Observabilidade
Este ponto refere-se ao grau no qual é possível observar os resultados intermédios e finais dos testes.

Para obter o resultado dos testes ao nível da camada da aplicação, é necessário corrê-los no [**Android Studio**](https://developer.android.com/studio/index.html). Os testes que exercitam as operações no servidor, devem ser corridos através do terminal, sendo para isso necessário possuir o [**JUnit**](http://junit.org/junit4/) e o [**Apache Ant**](http://ant.apache.org/bindownload.cgi). 

No entanto, alguns testes que estão no *branch master* (nomeadamente os que se encontram na pasta [**automationTest**](https://github.com/PauloSantos13/android/tree/master/automationTest)) estão obsoletos. Segundo a informação obtida junto do atual responsável pela aplicação, a equipa tem como objetivo produzir um conjunto mais alargado de testes [**Espresso**](https://google.github.io/android-testing-support-library/docs/espresso/index.html) que permitam abranger quase toda a aplicação. Existe já um *branch* denominado [**login_suite_espresso**](https://github.com/owncloud/android/tree/login_suite_espresso) onde estes testes se encontram a ser produzidos, mas não estão prontos para ser testados e, por esta razão, o grupo decidiu não os incluir neste relatório.

### Isolabilidade
A isolabilidade representa o grau em que cada componente pode ser testado isoladamente.

Assim, a isolabilidade é tanto maior quanto menor for a relação entre os módulos. No caso do *ownCloud*, a maior parte dos módulos estão relacionados entre si, o que dificulta o teste de cada um isoladamente.

### Separação de Responsabilidades
A separação de responsabilidades define se o componente a ser testado tem uma responsabilidade bem definida.

Para que a estrutura do projeto fique bem organizada e de fácil compreensão e acesso, cada módulo deve estar bem definido, evitando assim que o código fique misturado e menos eficiente. No caso do *ownCloud*, a sua estrutura está bem definida. Os principais desenvolvedores optaram por criar vários *packages* de forma a que cada funcionalidade fique bem definida, sendo os seus sub-problemas resolvidos no seu interior.

### Perceptibilidade
A perceptibilidade avalia o grau em que o componente em teste é autoexplicativo e está documentado.

Avaliando os testes disponíveis no projeto, determinamos que o nome dos mesmos é claro e, por isso, autoexplicativo. Isto permite ao utilizador verificar com muita facilidade qual o teste que falhou e a localização do erro.

### Heterogeneidade
A heterogeneidade indica a necessidade do projecto recorrer a diversas tecnologias para testar diferentes funcionalidades.

Numa fase inicial do projeto, existiam testes ao nível da interface que requeriam o uso da ferramenta [**Appium**](http://appium.io/slate/en/master/?java#about-appium) e das suas dependências. No momento da avaliação do projeto, estes já se encontram desatualizados e não fazem parte do conjunto de testes corridos, estando a ser convertidos em testes *Espresso* (ainda não concluídos). Desta forma, recorrem ao uso de *Apache Ant/JUnit3* para testar as operações ao nível do servidor e *Gradle* (desde que mudaram para o *Android Studio* como principal IDE), onde são corridos alguns testes ao nível da interface.

Assim, conclui-se que o projecto *ownCloud* é heterogéneo, uma vez que recorre a diversas tecnologias em paralelo.

## Estatísticas e análises dos testes

De uma forma geral, para avaliar a qualidade do *software*, recorrem-se a estatísticas de teste que tentam contemplar o maior número de componentes possível, determinando a eficiência e estabilidade do sistema. 

No caso particular do *ownCloud*, como foi referido no tópico anterior, o número de casos de teste é muito reduzido, havendo, por isso, uma percentagem de cobertura também muito reduzida.

Para testar esta percentagem de cobertura, tentou-se recorrer à ferramenta *Eclemma* do *Eclipse*, mas como o grupo não conseguiu correr o projeto neste IDE, deduzimos que esta percentagem seria muito reduzida devido ao baixo número de testes.

Tentou-se, também, utilizar as ferramentas *Cucumber*, *Infer* e *Hygieia*, referidas nas aulas teóricas, mas sem sucesso. Apenas se conseguiu usar [**Codacy**](https://www.codacy.com/), obtendo-se algumas estatísticas interessantes demonstradas a seguir:

![CodacyStats](/ESOF-docs/resources/codacy_stats.PNG)

Não foram detetados *flaky tests*, uma vez que correr os testes várias vezes com o mesmo código produz sempre o mesmo resultado.

Ao nível da camada de aplicação, existem apenas 5 testes que incidem sobre os pacotes *authentication*, *datamodel*, *uiautomator*, produzindo o seguinte resultado:

![GradleTests](/ESOF-docs/resources/gradle_tests.png)

Além dos testes ao nível da camada da aplicação, existe um outro conjunto de testes baseados em *JUnit3*, que exercitam a maior parte das operações que são possíveis realizar num servidor real do *ownCloud*. Para correr estes testes, foi necessário a instalação do *Apache Ant*, bem como a definição de algumas variáveis de ambiente requiridas pelos mesmos. 

Uma vez que a equipa segue uma prática de integração contínua, recorre ao [**Travis CI**](https://travis-ci.org/owncloud/android) para que, sempre que é realizado um *pull request*, estes testes sejam corridos, garantindo que as alterações que o código sofreu não alteraram estas funcionalidades.

O grupo correu estes testes, e obteve o seguinte resultado:

```
-setup:
     [echo] Project Name: ownCloud Android library test cases
  [gettype] Project Type: Test Application

-test-project-check:

test:
     [echo] Running tests ...
     [exec] 
     [exec] com.owncloud.android.lib.test_project.test.CopyFileTest:.
     [exec] com.owncloud.android.lib.test_project.test.CreateFolderTest:..
     [exec] com.owncloud.android.lib.test_project.test.CreateShareTest:
     [exec] Failure in testCreateFederatedShareWithUser:
     [exec] junit.framework.AssertionFailedError
     [exec] 	at com.owncloud.android.lib.test_project.test.CreateShareTest.testCreateFederatedShareWithUser(CreateShareTest.java:238)
     [exec] 	at android.test.InstrumentationTestCase.runMethod(InstrumentationTestCase.java:214)
     [exec] 	at android.test.InstrumentationTestCase.runTest(InstrumentationTestCase.java:199)
     [exec] 	at android.test.ActivityInstrumentationTestCase2.runTest(ActivityInstrumentationTestCase2.java:192)
     [exec] 	at android.test.AndroidTestRunner.runTest(AndroidTestRunner.java:191)
     [exec] 	at android.test.AndroidTestRunner.runTest(AndroidTestRunner.java:176)
     [exec] 	at android.test.InstrumentationTestRunner.onStart(InstrumentationTestRunner.java:555)
     [exec] 	at android.app.Instrumentation$InstrumentationThread.run(Instrumentation.java:1886)
     [exec] ...
     [exec] com.owncloud.android.lib.test_project.test.DeleteFileTest:..
     [exec] com.owncloud.android.lib.test_project.test.DownloadFileTest:...
     [exec] com.owncloud.android.lib.test_project.test.GetCapabilitiesTest:.
     [exec] com.owncloud.android.lib.test_project.test.GetShareesTest:.
     [exec] Failure in testGetRemoteShareesOperation:
     [exec] junit.framework.AssertionFailedError
     [exec] 	at com.owncloud.android.lib.test_project.test.GetShareesTest.testGetRemoteShareesOperation(GetShareesTest.java:131)
     [exec] 	at android.test.InstrumentationTestCase.runMethod(InstrumentationTestCase.java:214)
     [exec] 	at android.test.InstrumentationTestCase.runTest(InstrumentationTestCase.java:199)
     [exec] 	at android.test.ActivityInstrumentationTestCase2.runTest(ActivityInstrumentationTestCase2.java:192)
     [exec] 	at android.test.AndroidTestRunner.runTest(AndroidTestRunner.java:191)
     [exec] 	at android.test.AndroidTestRunner.runTest(AndroidTestRunner.java:176)
     [exec] 	at android.test.InstrumentationTestRunner.onStart(InstrumentationTestRunner.java:555)
     [exec] 	at android.app.Instrumentation$InstrumentationThread.run(Instrumentation.java:1886)
     [exec] 
     [exec] com.owncloud.android.lib.test_project.test.GetSharesTest:
     [exec] Failure in testGetShares:
     [exec] junit.framework.AssertionFailedError
     [exec] 	at com.owncloud.android.lib.test_project.test.GetSharesTest.testGetShares(GetSharesTest.java:79)
     [exec] 	at android.test.InstrumentationTestCase.runMethod(InstrumentationTestCase.java:214)
     [exec] 	at android.test.InstrumentationTestCase.runTest(InstrumentationTestCase.java:199)
     [exec] 	at android.test.ActivityInstrumentationTestCase2.runTest(ActivityInstrumentationTestCase2.java:192)
     [exec] 	at android.test.AndroidTestRunner.runTest(AndroidTestRunner.java:191)
     [exec] 	at android.test.AndroidTestRunner.runTest(AndroidTestRunner.java:176)
     [exec] 	at android.test.InstrumentationTestRunner.onStart(InstrumentationTestRunner.java:555)
     [exec] 	at android.app.Instrumentation$InstrumentationThread.run(Instrumentation.java:1886)
     [exec] 
     [exec] com.owncloud.android.lib.test_project.test.GetUserAvatarTest:
     [exec] Failure in testGetUserAvatar:
     [exec] junit.framework.AssertionFailedError
     [exec] 	at com.owncloud.android.lib.test_project.test.GetUserAvatarTest.testGetUserAvatar(GetUserAvatarTest.java:62)
     [exec] 	at android.test.InstrumentationTestCase.runMethod(InstrumentationTestCase.java:214)
     [exec] 	at android.test.InstrumentationTestCase.runTest(InstrumentationTestCase.java:199)
     [exec] 	at android.test.ActivityInstrumentationTestCase2.runTest(ActivityInstrumentationTestCase2.java:192)
     [exec] 	at android.test.AndroidTestRunner.runTest(AndroidTestRunner.java:191)
     [exec] 	at android.test.AndroidTestRunner.runTest(AndroidTestRunner.java:176)
     [exec] 	at android.test.InstrumentationTestRunner.onStart(InstrumentationTestRunner.java:555)
     [exec] 	at android.app.Instrumentation$InstrumentationThread.run(Instrumentation.java:1886)
     [exec] ..
     [exec] Error in testGetUserAvatarOnlyIfChangedAfterUnchanged:
     [exec] java.lang.NullPointerException: Attempt to invoke virtual method 'java.lang.Object java.util.ArrayList.get(int)' on a null object reference
     [exec] 	at com.owncloud.android.lib.test_project.test.GetUserAvatarTest.testGetUserAvatarOnlyIfChangedAfterUnchanged(GetUserAvatarTest.java:74)
     [exec] 	at android.test.InstrumentationTestCase.runMethod(InstrumentationTestCase.java:214)
     [exec] 	at android.test.InstrumentationTestCase.runTest(InstrumentationTestCase.java:199)
     [exec] 	at android.test.ActivityInstrumentationTestCase2.runTest(ActivityInstrumentationTestCase2.java:192)
     [exec] 	at android.test.AndroidTestRunner.runTest(AndroidTestRunner.java:191)
     [exec] 	at android.test.AndroidTestRunner.runTest(AndroidTestRunner.java:176)
     [exec] 	at android.test.InstrumentationTestRunner.onStart(InstrumentationTestRunner.java:555)
     [exec] 	at android.app.Instrumentation$InstrumentationThread.run(Instrumentation.java:1886)
     [exec] 
     [exec] com.owncloud.android.lib.test_project.test.GetUserQuotaTest:.
     [exec] com.owncloud.android.lib.test_project.test.MoveFileTest:.
     [exec] com.owncloud.android.lib.test_project.test.OwnCloudClientManagerFactoryTest:.....
     [exec] com.owncloud.android.lib.test_project.test.OwnCloudClientTest:...........
     [exec] com.owncloud.android.lib.test_project.test.ReadFileTest:
     [exec] Failure in testReadFile:
     [exec] junit.framework.AssertionFailedError
     [exec] 	at com.owncloud.android.lib.test_project.test.ReadFileTest.testReadFile(ReadFileTest.java:70)
     [exec] 	at android.test.InstrumentationTestCase.runMethod(InstrumentationTestCase.java:214)
     [exec] 	at android.test.InstrumentationTestCase.runTest(InstrumentationTestCase.java:199)
     [exec] 	at android.test.ActivityInstrumentationTestCase2.runTest(ActivityInstrumentationTestCase2.java:192)
     [exec] 	at android.test.AndroidTestRunner.runTest(AndroidTestRunner.java:191)
     [exec] 	at android.test.AndroidTestRunner.runTest(AndroidTestRunner.java:176)
     [exec] 	at android.test.InstrumentationTestRunner.onStart(InstrumentationTestRunner.java:555)
     [exec] 	at android.app.Instrumentation$InstrumentationThread.run(Instrumentation.java:1886)
     [exec] 
     [exec] com.owncloud.android.lib.test_project.test.ReadFolderTest:
     [exec] Failure in testReadFolder:
     [exec] junit.framework.AssertionFailedError
     [exec] 	at com.owncloud.android.lib.test_project.test.ReadFolderTest.testReadFolder(ReadFolderTest.java:84)
     [exec] 	at android.test.InstrumentationTestCase.runMethod(InstrumentationTestCase.java:214)
     [exec] 	at android.test.InstrumentationTestCase.runTest(InstrumentationTestCase.java:199)
     [exec] 	at android.test.ActivityInstrumentationTestCase2.runTest(ActivityInstrumentationTestCase2.java:192)
     [exec] 	at android.test.AndroidTestRunner.runTest(AndroidTestRunner.java:191)
     [exec] 	at android.test.AndroidTestRunner.runTest(AndroidTestRunner.java:176)
     [exec] 	at android.test.InstrumentationTestRunner.onStart(InstrumentationTestRunner.java:555)
     [exec] 	at android.app.Instrumentation$InstrumentationThread.run(Instrumentation.java:1886)
     [exec] 
     [exec] com.owncloud.android.lib.test_project.test.RemoveShareTest:.
     [exec] com.owncloud.android.lib.test_project.test.RenameFileTest:....
     [exec] com.owncloud.android.lib.test_project.test.SimpleFactoryManagerTest:....
     [exec] com.owncloud.android.lib.test_project.test.SingleSessionManagerTest:....
     [exec] com.owncloud.android.lib.test_project.test.UpdatePrivateShareTest:.
     [exec] com.owncloud.android.lib.test_project.test.UpdatePublicShareTest:.
     [exec] com.owncloud.android.lib.test_project.test.UploadFileTest:...
     [exec] Test results for InstrumentationTestRunner=....F...........F.F.F...E...................F.F.
     [exec] .................
     [exec] Time: 413.235
     [exec] 
     [exec] FAILURES!!!
     [exec] Tests run: 58,  Failures: 6,  Errors: 1
     [exec] 
     [exec] 

```

Verifica-se que, dos 58 testes existentes, 6 falham e ocorre 1 erro num deles.
Posto isto, pode-se deduzir que existem testes que exercitam componentes que podem já ter sido alterados e, desta forma, encontram-se desatualizados, levando à sua falha.

Numa perspetiva geral, a maior parte dos componentes da camada da aplicação não possui (atualmente) testes implementados. Pode-se concluir que esta falta de testes é um defeito do projeto, pois dificulta a validação da maioria dos módulos.

## Identificação de um novo *bug* e/ou correção do *bug*
Após alguns testes à aplicação, não detetámos nenhum *bug* novo. Uma vez que a maioria dos *bugs* que a aplicação possui foram  reportados pelos diversos utilizadores e ainda não foram corrigidos, decidimos resolver um dos indicados nas *issues* do *GitHub*, escolhendo a [***issue* 1562**](https://github.com/owncloud/android/issues/1562).

Esta *issue* indica que quando um ficheiro é partilhado com um utilizador e, depois, é partilhado usando uma hiperligação pública, se se fechar a aplicação e voltar a abrir e abrir as opções de partilha do ficheiro para tentar copiar a hiperligação, nenhuma hiperligação é retornada.

Aqui registam-se as alterações feitas para resolver a *issue* (foi retirada a linha a vermelho, substituída pelas seguintes).

![CodeChangedOnShareActivity](/ESOF-docs/resources/code_changed_on_share_activity.PNG)

Para resolver a *issue* 1562, analisamos, inicialmente, a situação usando a interface do utilizador e, posteriormente, o código. Ao nível da interface, verificámos que o problema ocorria tanto com ficheiros como com pastas e que também ocorria se, logo que realizássemos a partilha, pedíssemos a hiperligação duas vezes; a primeira funcionava bem, mas, a partir da segunda vez, já não. 

Ao nível do código, verificámos que, por vezes, o *ArrayList* retornado pelo método “getData” em “ShareActivity” nem sempre tinha apenas um elemento, tendo mais do que um, isto é, tinha uma hiperligação para *User* e uma para *Public*. 

Depois, analisámos o código da classe “RemoteOperationResult”, que é usada por diversas classes, e descobrimos que possuía um método chamado “setData”. De seguida, identificámos, com a ajuda da capacidade “Find Usages” do *Android Studio*, todos os locais em que “setData” era usado e verificámos que apenas as classes “ShareToRemoteOperationResultParser” e “ReadRemoteFolderOperation” poderiam criar um *ArrayList* com mais de um elemento. 

Após analisarmos o que essas duas classes adicionavam ao *ArrayList* delas, antes de ser aplicado o método “setData”, entendemos que o problema não estava em “ReadRemoteFolderOperation”, pois esta classe trata os dados de uma pasta remota e seus ficheiros-filhos. Portanto, verificamos se havia diferentes tipos de hiperligações e descobrimos em “ShareType” que isso era verdade, sendo muito perigoso apenas aceitar hiperligações públicas em “ShareToRemoteOperationResultParser” (sabemos agora que de certeza que isso iria trazer problemas), portanto decidimos usar uma solução que se aparenta com a apresentada na *issue*, mas que achamos melhor que esse *workaround*. Achamos esta a melhor solução para o problema, não trazendo efeitos negativos ao funcionamento da aplicação.

Mais tarde, confirmamos que, como esperado, as hiperligações eram adicionadas por ordem de criação, então, se a hiperligação pública fosse a primeira a ser criada (ou não fosse criada), nunca havia problema, caso contrário, o problema ocorreria.

*Pull request*: https://github.com/owncloud/android/pull/1844

## Contribuições

Diogo Cruz - up201105483@fe.up.pt - 25%

Luís Barbosa - up201405729@fe.up.pt - 25%

Paulo Santos - up201403745@fe.up.pt - 25%

Sérgio Ferreira - up201403074@fe.up.pt - 25%
