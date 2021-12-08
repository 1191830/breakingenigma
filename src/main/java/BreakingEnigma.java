
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

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
    static String enigma= "";
    static boolean isPlugged = false;
    static boolean found = false;
    static String myHash ="";
    static int rot;
    static int shi;
    static String salt;
    static String saltPos;
    static String hashAlgorithm;
    static HashMap<Character,Character> plugMap = new HashMap<>();
    
    
    public static void main(String[] args) throws IOException{
        if(args.length != 3){
            System.out.println("Por favor introduza 3 argumentos ( hash, plug, txt)");
        }else if (checkHash(args[0]) && plugboardConvert(args[1])){
            ArrayList<String> words;

            myHash = args[0];
            words = readFile(args[2]);

            runWords(words);

            if(found){
                System.out.println("ENCONTRADO");
                System.out.println(enigma + " " + saltPos + " " + salt + " "+ rot + " " + shi);
                System.out.println("Deseja guardar em CSV?");
                String op = sc.nextLine();
                if (op.equals("s")){
                    toCsv();
                }
            }else{
                System.out.println("NAO EXISTE");
            } 
        }
      
    }
    
    public static void toCsv() throws IOException{
        FileWriter writer = new FileWriter("./pass.csv");
        writer.append("Pass; " + enigma);
        writer.append("\n");
        writer.append("Salt Pos; " + saltPos);
        writer.append("\n");
        writer.append("Salt word; " + salt);
        writer.append("\n");
        writer.append("Rotation; " + rot);
        writer.append("\n");
        writer.append("Shift; " + shi);
        writer.append("\n");
        writer.append("Hash; " + myHash);
        writer.close();
    }
    
    public static ArrayList<String> readFile(String path) {
        FileReader r = null;
        BufferedReader b = null;
        ArrayList<String> fileWords = new ArrayList<>();
        String currentDir = System.getProperty("user.dir") + "\\.\\";
        path = path.replace("./", "");
        path = currentDir + path;
        try {
            File file = new File(path);
            path = file.getAbsolutePath();

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
    
    public static boolean checkHash(String hash){
        int count = 0;
        for (int i = 0; i < hash.length(); i++) {
            if(!Character.isLetterOrDigit(hash.charAt(i))){
                System.out.println(hash.charAt(i));
               return false; 
            }
            count ++;
        }
        
        switch (count){
            case 128:
                hashAlgorithm = "SHA-512";
                break;
            case 64:
                hashAlgorithm = "SHA-256";
                break;
            case 32:
                hashAlgorithm = "SHA-128";
                break;
            default:
                System.out.println("Hash não é valida");
                return false;               
        }
        return true;
    }
    
    public static String runWords(ArrayList<String> words){
         for(String word : words){
            System.out.println(word);
            addSalt(word); 
            if(found){
                enigma = word;
                break;
            }
        }
        return enigma;
    }
    
    private static void addSalt(String word) {
            String saltDic = "ABCDEFGHIJKLM";
            
            for (int i = 0; i < saltDic.length(); i++) {
                    if(found){
                        break;
                    }
                    salt = String.valueOf(saltDic.charAt(i));
                
                for (int j = 0; j < saltDic.length(); j++) {
                    salt += String.valueOf(saltDic.charAt(j));
                    plugboard(String.format("%s%s%s",salt,word,salt));
                    if(found){;
                        break;
                    }            
                    salt = String.valueOf(saltDic.charAt(i));
                }
                
            }
    }
    
    public static void plugboard(String message){
        
        StringBuilder plugged = new StringBuilder();
        for (char c : message.toCharArray()){
             if (plugMap.containsKey(c)){
                  plugged.append(plugMap.get(c));
             }else {
                  plugged.append(c);
             }
        }
        if(!isPlugged){
            caesarPalace(plugged.toString());
        }
        isPlugged = false;
        verifyWord(plugged.toString());
        
    }
    
    public static void caesarPalace(String plugged){
        for (int i = 0; i < 26; i++) {
            if(found){
                break;
            }
            for (int j = 0; j < 26; j++) {
                caesar(plugged, i, j);
                if(found){
                    rot = i;
                    shi = j;
                    break;
                }
            }           
        }
    }
    
    public static void caesar(String message1, int rotation, int shift){
            if(rotation == 0 && shift == 0){
                isPlugged = true;
                plugboard(message1);
            }
            String caesar1= "";
            String caesar2= "";
            String message2 = message1.substring(2, message1.length());
            message1 = message1.substring(0, message1.length()-2);
            int inc = 0;
            String abc = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
            
            for (int i = 0; i < message1.length(); i++) {
                char letter1 = message1.charAt(i);
                char letter2 = message2.charAt(i);
                
                if(Character.isLetter(letter1)){
                    inc = shift * i;
                    inc = inc + rotation;
                    
                    int index1 = abc.indexOf(letter1);
                    int index2 = abc.indexOf(letter2);
                    int newIndex1 = (index1 + inc) % 26;
                    int newIndex2 = (index2 + inc) % 26;
                    
                    char ch = abc.charAt(newIndex1);                   
                    caesar1 += ch;
                    ch = abc.charAt(newIndex2);
                    caesar2 += ch;
                }
            }
            //return caesar;
            isPlugged = true;
            caesar1 += caesar2;
            plugboard(caesar1);
        }
    
    public static void verifyWord(String word){
        String encrypted0 = encrypt(word.substring(0, word.length()/2));
        String encrypted1 = encrypt(word.substring(word.length()/2, word.length()));
        
        if(encrypted0.equals(myHash)){
            found = true;
            saltPos = "0";
        }else if (encrypted1.equals(myHash)){
            found = true;
            saltPos = "1";            
        }
    }
    
    public static String encrypt(String encrypt){
        StringBuilder sb = new StringBuilder();
        try {
            MessageDigest md = MessageDigest.getInstance(hashAlgorithm);
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
    
    public static boolean plugboardConvert(String map){
        map = map.substring(1, map.length() - 1);
        String[] keyValuePairs = map.split(",");

        try {    
            for(String pair : keyValuePairs)                        //iterate over the pairs
            {
                String[] entry = pair.split(":");                  //split the pairs to get key and value
                String keys = entry[0].replace("\'", "");
                String values = entry[1].replace("\'", "");
                char key = keys.trim().charAt(0);
                char value = values.trim().charAt(0);
                if(Character.isLetter(key) && Character.isLetter(value)){
                   plugMap.put(key, value);          //add them to the hashmap and trim whitespaces 
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
    
}
