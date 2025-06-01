import java.util.LinkedList;
import java.util.Queue;
import java.io.BufferedReader;
import java.io.IOException;

class CustomFileReader {
    private BufferedReader reader;
    // private StringTokenizer tokenizer;
    private Queue<String> tokens;
    public String word;
    public String previousWord;
    public boolean isFirstWordInLine = false;
    public boolean updateIsFirstWordInLine = true; // whether to set isFirstWordInLine to true or not. This is required when the word after the last literal is read in ltorg, and put back into the tokens queue. This retains the isFirstWordInLine value.

    // read the input file word by word
    public CustomFileReader(String filename) throws IOException {
        reader = new BufferedReader(new java.io.FileReader(filename));
        tokens = new LinkedList<>();
    }

    public String readWord() throws IOException {
        if (tokens.isEmpty()) {
            String line = reader.readLine();
            if (line == null) {
                reader.close();
                return null; // end of file
            }
            isFirstWordInLine = true; // Set to true when a new line is read
            String[] words = line.split("\\s+");
            for (String word : words) {
                if (!word.isBlank()) {
                    tokens.add(word);
                }
            }
            previousWord = null;
            word = tokens.poll();
            return word;
        }
        previousWord = word;
        word = tokens.poll();
        if (updateIsFirstWordInLine && isFirstWordInLine) {
            isFirstWordInLine = false; // Reset after the first word in the line is read
        }
        updateIsFirstWordInLine = true;
        return word;
    }

    public void addWordToQueueBeginning(String word) {
        ((LinkedList<String>) tokens).addFirst(word);
    }

    public void close() throws IOException {
        reader.close();
    }
}
