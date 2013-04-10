package storage;

public interface StorageInterface
{
   public String write(String filename, String key, String value);
   public String read(String filename, String key);
}

