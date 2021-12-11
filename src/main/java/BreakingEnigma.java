
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Rui
 */
public class BreakingEnigma {
    
    static Scanner sc = new Scanner(System.in);
    //password encontrada
    static String enigma= "";
    //hash que vamos receber como argumento
    static String myHash ="";
    //rotaçao e shift dos rotores
    static int rot;
    static int shi;
    //Salt para mostrar ao utilizador e posiçao 0=inicio 1=fim
    static String salt;
    static String saltPos;
    //Algoritmo de hash
    static String hashAlgorithm;
    //Array para o salt e para o calculo do incremento
    static char[] saltDic = "ABCDEFGHIJKLM".toCharArray();
    static String abc = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    //plugboard
    static HashMap<Character,Character> plugMap = new HashMap<>();
    
    //Constantes para verificar os algoritmso da hash
    static final int SHA512 = 128;
    static final int SHA256 = 64;
    static final int MD5 = 32;
      
    public static void main(String[] args) throws IOException{
        if(args.length != 3){
            System.out.println("Por favor introduza 3 argumentos ( hash, plug, txt)");
        }else if (checkHash(args[0]) && plugboardConvert(args[1])){
            ArrayList<String> words;

            myHash = args[0];
            words = readFile(args[2]);

            runWords(words);

            System.out.println("NAO EXISTE");    
        }
    }
    
    /*
    ############################################################################
                        VERIFICAÇÕES ARGUMENTOS
    ############################################################################
    */
    
    /**
     * Recebe uma string e verifica se é uma hash válida (Só contém números e 
     * letras), depois verifica se tamanho corresponde a algoritmo presente na 
     * lista de constantes
     * @param hash
     * @return boolean
     */
    public static boolean checkHash(String hash){
        //regex de numeros e letras (maiusculas e minusculas)
        Pattern p = Pattern.compile("^[a-zA-Z0-9]+$");
        Matcher m = p.matcher(hash);
        //se corresponder ao regex podemos verificar se algoritmo está presente
        if(m.matches()){
            switch (hash.length()){
                case SHA512:
                    hashAlgorithm = "SHA-512";
                    break;
                case SHA256:
                    hashAlgorithm = "SHA-256";
                    break;
                case MD5:
                    hashAlgorithm = "MD5";
                    break;
                default:
                    System.out.println("Hash não é valida");
                    return false;               
            }
            return true;
        }
        System.out.println("Hash não é válida");
        return false;
    }
    
    /**
     * Recebe uma string e transforma em hashmap(ex: {A:B, C:D} )
     * @param map
     * @return boolean
     */
    public static boolean plugboardConvert(String map){
        //Se a String tiver {} são retiradas
        map = map.replace("{", "");
        map = map.replace("}", "");
        //Separa todos os valores separados por virgulas
        String[] keyValuePairs = map.split(",");

        try {    
            for(String pair : keyValuePairs)                        
            {
                String[] entry = pair.split(":");
                //Se tirver pelicas são retiradas
                String keys = entry[0].replace("\'", "");
                String values = entry[1].replace("\'", "");
                //Atribuimos o primeiro caracter como chave e o segundo como valor para o hashmap
                char key = keys.trim().charAt(0);
                char value = values.trim().charAt(0);
                //Só queremos transformar letras, se tivermos outra coisa nã é válido
                if(Character.isLetter(key) && Character.isLetter(value)){
                   plugMap.put(key, value);          
                }else{
                    System.out.println("PlugBoard incorreta");
                    return false;
                }               
            }
            return true;
         }
         catch (Exception e) { 
             System.out.println("PlugBoard incorreta");
             return false;
        }
    }

    /*
    ############################################################################
                        LÓGICA
    ############################################################################
    */
    
    /**
     * Recebe um arraylist de Strings e percorre-o até encontrar a password
     * @param words
     * @return boolean
     * @throws IOException 
     */
    public static boolean runWords(ArrayList<String> words) throws IOException{
         for(String word : words){
             //Pode ser comentado, serve para mostrar a palavra atual
            System.out.println(word);           
            //envia a palavra atual diretamente para o sistema de rotor
            if(caesarPalace(word)){
                //se encontrar atribui a palavra atual a uma variavel global
                enigma = word;
                //mostra os resultados e pergunta se quer gravar em CSV
                found();
                return true;
            }
        }
        return false;
    }
    
    /**
     * Recebe uma String que será utilizada pelos rotores e adicionado salts
     * @param plugged
     * @return boolean
     * @throws IOException 
     */
    public static boolean caesarPalace(String plugged) throws IOException{
        //A primeira vez que passa pelos rotores a palavra é sempre igual não precisamos
        //de passá-la por todas as rotações
        String word = plugboard(plugged);
        
        //Brute Force ao sistema de rotação e Shift
        for (rot = 0; rot < 26; rot++) {
            for (shi = 0; shi < 26; shi++) {
                //Primeiro fazemos o processo com a palvra e depois juntamos todos
                //os salts à palvra final gerada
                enigma = caesar(word, rot, shi);
                enigma = plugboard(enigma);
                //Caso encontre a palavra num dos salts o programa retorna true e termina
                if(addSalt(enigma)){
                    return true;
                }
            }           
        }
        return false;
    }
    
    /**
     * Recebe String e altera letras mediante um plugboard
     * @param message
     * @return String
     * @throws IOException 
     */
    public static String plugboard(String message) throws IOException{
        
        String plugged = "";
        for (char c : message.toCharArray()){
            //Se o caracter estiver nos valores Key do hashmap muda para o value
            //correspondente senão fica igual
             if (plugMap.containsKey(c)){
                  plugged += plugMap.get(c);
             }else {
                  plugged += c;
             }
        }
        return plugged;       
    }
    
    /**
     * Recebe palavra rotaçao e shift e concatena a palavra simulando o salt inicial e final
     * @param message1
     * @param rotation
     * @param shift
     * @return String
     * @throws IOException 
     */
    public static String caesar(String message1, int rotation, int shift) throws IOException{
        //Se a rotação e Shift forem 0 não é preciso rodar
            if(rotation == 0 && shift == 0){
                //Returnamos 2 palavras para serem concatenadas com  o salt esquerdo e direito
                return message1 + message1;
            }
            String caesar1= "";
            String caesar2= "";
            //incremento para simular salt esquerdo e direito
            int inc1 = 0;
            int inc2 = 0;
            
            for (int i = 0; i < message1.length(); i++) {
                char letter = message1.charAt(i);
                
                //para o caso de haver no ficheiro palavras com algo que não letras
                if(Character.isLetter(letter)){
                    //A palavra 1 vai ter o salt inicial(ex AA) logo o index 0 é na verdade 0 + 2
                    inc1 = shift * (i + 2);
                    inc1 = inc1 + rotation;
                    
                    //Já a palavra 2 só tem no fim logo o index 0 = 0
                    inc2 = shift * i;
                    inc2 = inc2 + rotation;
                    
                    int index = abc.indexOf(letter);
                    
                    char ch = abc.charAt((index + inc1) % 26);                   
                    caesar1 += ch;
                    ch = abc.charAt((index + inc2) % 26);
                    caesar2 += ch;
                }
            }
            //palavras prontas para o salt inicial e final
            caesar1 += caesar2;
            
            return caesar1;
        }
    
    /**
     * Recebe String, adiciona 2 caracteres e verifica se corresponde À hash,
     * após passarem no ciclo plugboard Caesar plugboard
     * @param word
     * @return boolean
     * @throws IOException 
     */
    private static boolean addSalt(String word) throws IOException {
            
        //Percorre o array de letras
            for (char letter1 : saltDic) {
                for (char letter2 : saltDic) {
                    salt = String.valueOf(letter1) + letter2;
                    //A primeira iteração só precisa dos 2 caracteres
                    salt = plugboard(salt);
                    //sistema de rotaçao com o rot e shift atual, a palavra será
                    //dividida em 2 salt inicial e final
                    salt = caesarSalt(salt, rot, shi);
                    //salt inicial e final concatenado para a 2 iteração do plugboard
                    salt = plugboard(salt);
                    //Concateanar o salt e a palvra dso lados correspondentes
                    if(verifyWord((salt.substring(0, 2) + word.substring(0, word.length()/2)), 
                            (word.substring(word.length()/2, word.length()) + salt.substring(2, salt.length())))){
                        //Se encontrarmos a palavra atribuimos o hash encontrado
                        salt = String.valueOf(letter1) + letter2;
                        return true;
                    }
                }               
            }
            return false;
    }
    
        /**
         * Recebe String, rotacao e shift e concatena os salts simulando as palavras
         * Similar ao Caesar normal mas adaptado ao tamanh dos salts
         * @param message1
         * @param rotation
         * @param shift
         * @return
         * @throws IOException 
         */
        public static String caesarSalt(String message1, int rotation, int shift) throws IOException{
            if(rotation == 0 && shift == 0){
                return message1 + message1;
            }
            String caesar1= "";
            String caesar2= "";
            int inc1 = 0;
            int inc2 = 0;
            
            for (int i = 0; i < message1.length(); i++) {
                char letter = message1.charAt(i);
                
                if(Character.isLetter(letter)){
                    //O primeiro salt fica igual pois o index é o mesmo
                    inc1 = shift * i;
                    inc1 = inc1 + rotation;
                    
                    //O segundo salt simula a presença da palavra logo index 0 = 0 + tamanho da palavra(6)
                    inc2 = shift * (i + 6);
                    inc2 = inc2 + rotation;
                    
                    int index = abc.indexOf(letter);
                    
                    char ch = abc.charAt((index + inc1) % 26);                   
                    caesar1 += ch;
                    ch = abc.charAt((index + inc2) % 26);
                    caesar2 += ch;
                }
            }
            caesar1 += caesar2;
            
            return caesar1;
        }
    
        /**
         * Recebe duas string, encripta e compara com a hash
         * @param encrypted0
         * @param encrypted1
         * @return boolean
         * @throws IOException 
         */
    public static boolean verifyWord(String encrypted0, String encrypted1) throws IOException{
        //Palavras com hash no inicio e fim
        encrypted0 = encrypt(encrypted0);
        encrypted1 = encrypt(encrypted1);
        
        //Se encontrar a hash retorna a posição do salt mediante o corrento
        if(encrypted0.equals(myHash)){
            saltPos = "0";
            return true;
        }else if (encrypted1.equals(myHash)){
            saltPos = "1";
            return true;
        }
        return false;
    }
    
    /**
     * Recebe String e encripta usando o algoritmo
     * @param encrypt
     * @return 
     */
    public static String encrypt(String encrypt){
        StringBuilder sb = new StringBuilder();
        try {
            //Recebe o algoritmo defenido na váriavel global, foi determinado pelo
            //tamanho da hash recebida como argumento
            MessageDigest md = MessageDigest.getInstance(hashAlgorithm);
            //Transforma a nossa String num array de Bytes
            byte[] data = md.digest(encrypt.getBytes());
            
            for(int i=0;i<data.length;i++)
            {
                sb.append(Integer.toString((data[i] & 0xff) + 0x100, 16).substring(1));
            }

         } catch(Exception e) {
            System.out.println(e); 
         }
        return sb.toString();
    }
    
    public static void found() throws IOException{
        System.out.println("ENCONTRADO");
        System.out.println(enigma + " " + saltPos + " " + salt + " "+ rot + " " + shi);
        System.out.println("Deseja guardar em CSV?");
        String op = sc.nextLine().toLowerCase();
        if (op.equals("s")){
               toCsv();
        }
        System.exit(0);
    }

    /*
    ############################################################################
                        LEITURA E ESCRITA DE FICHEIROS
    ############################################################################
    */
    public static void toCsv() throws IOException{
        try(FileWriter fw = new FileWriter("./pass.csv", true);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter out = new PrintWriter(bw))
        {           
            out.println("Password ; " + enigma);
            out.println("Salt ; " + salt);
            out.println("Salt position ; " + saltPos);
            out.println("Rotation ; " + rot);
            out.println("Shift ; " + shi);
            out.println("Hash Algorithm ; " + hashAlgorithm);
            out.println("Hash ; " + myHash);           
            out.println("\n");
            
        } catch (IOException e) {
            //exception handling left as an exercise for the reader
        }
    }
    
    /**
     * Recebe String e lê ficheiro no caminho indicado pela String
     * @param path
     * @return 
     */
    public static ArrayList<String> readFile(String path) {
        FileReader r = null;
        BufferedReader b = null;
        ArrayList<String> fileWords = new ArrayList<>();
        //Recebe o diretorio actual e acrescenta /./ para o diretorio do ficheiro
        String currentDir = System.getProperty("user.dir") + "\\.\\";
        //retiramos ./ que o user possa introduzir
        path = path.replace("./", "");
        //concatena o diretorio atual com a String recebida
        path = currentDir + path;
        try {
            File file = new File(path);

            r = new FileReader(path);
            b = new BufferedReader(r);
            String s;
            while((s = b.readLine()) != null){
                fileWords.add(s);
            }
            
            r.close();
            b.close();
            
        } catch (Exception ex) {
            System.out.println("O caminho do ficheiro não é valido");
        }
        
        return fileWords;
    }
}
