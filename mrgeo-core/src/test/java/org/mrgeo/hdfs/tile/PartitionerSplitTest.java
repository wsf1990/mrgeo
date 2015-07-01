package org.mrgeo.hdfs.tile;

import junit.framework.Assert;
import org.apache.hadoop.fs.Path;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;
import org.mrgeo.hdfs.partitioners.SplitGenerator;
import org.mrgeo.junit.UnitTest;

import java.io.*;
import java.util.*;

public class PartitionerSplitTest
{
  static Long[] generated = new Long[]{1L, 2L, 5L, 10L, 15L, 20L, 30L, 50L};
  @Rule public TemporaryFolder folder = new TemporaryFolder();
  @Rule public TestName testName = new TestName();

  @Test
  @Category(UnitTest.class)
  public void generateSplits() {
    Splits splits = new PartitionerSplit();
    splits.generateSplits(new TestGenerator());

    PartitionerSplit.PartitionerSplitInfo[] si = (PartitionerSplit.PartitionerSplitInfo[]) splits.getSplits();

    Assert.assertEquals("Splits length not correct", generated.length, si.length);
    for (int i = 0; i < generated.length; i++)
    {
      Assert.assertEquals("Splits entry not correct", generated[i].longValue(), si[i].getTileId());
    }
  }

  @Test
  @Category(UnitTest.class)
  public void getSplitsNoGenerate()
  {
    Splits splits = new PartitionerSplit();

    PartitionerSplit.PartitionerSplitInfo[] si = (PartitionerSplit.PartitionerSplitInfo[]) splits.getSplits();

    Assert.assertNull("Splits not null", si);
  }

  @Test
  @Category(UnitTest.class)
  public void getLength()
  {
    Splits splits = new PartitionerSplit();
    splits.generateSplits(new TestGenerator());

    Assert.assertEquals("Splits length not correct", generated.length, splits.length());
  }

  @Test
  @Category(UnitTest.class)
  public void getSplit() throws Splits.SplitException
  {
    Splits splits = new PartitionerSplit();
    splits.generateSplits(new TestGenerator());

    Random rand = new Random();
    for (int i = 0; i < 1000; i++)
    {
      long test = rand.nextInt(generated[generated.length - 1].intValue());
      long testPartition = findSplit(generated, test);

      PartitionerSplit.PartitionerSplitInfo split =
          (PartitionerSplit.PartitionerSplitInfo) splits.getSplit(test);

      Assert.assertEquals("Splits entry not correct", testPartition, split.getPartition());
    }
  }

  @Test
  @Category(UnitTest.class)
  public void getSplitLow() throws Splits.SplitException
  {
    Splits splits = new PartitionerSplit();
    splits.generateSplits(new TestGenerator());

    PartitionerSplit.PartitionerSplitInfo split =
        (PartitionerSplit.PartitionerSplitInfo) splits.getSplit(0);

    Assert.assertEquals("Splits entry not correct", 0, split.getPartition());
  }

  @Test(expected = Splits.SplitException.class)
  @Category(UnitTest.class)
  public void getSplitHigh() throws Splits.SplitException
  {
    Splits splits = new PartitionerSplit();
    splits.generateSplits(new TestGenerator());

    splits.getSplit(1000);
  }

  @Test(expected = Splits.SplitException.class)
  @Category(UnitTest.class)
  public void getSplitNotGenerated() throws Splits.SplitException
  {
    Splits splits = new PartitionerSplit();
    splits.getSplit(10);
  }

  @Test
  @Category(UnitTest.class)
  public void getSplitLotsOfPartitions() throws Splits.SplitException
  {
    Splits splits = new PartitionerSplit();
    BigTestGenerator btg = new BigTestGenerator();
    splits.generateSplits(btg);

    Random rand = new Random();
    for (int i = 0; i < 10000; i++)
    {
      long test = rand.nextInt(btg.generated[btg.generated.length - 1].intValue());
      long testPartition = findSplit(btg.generated, test);

      PartitionerSplit.PartitionerSplitInfo split =
          (PartitionerSplit.PartitionerSplitInfo) splits.getSplit(test);

      Assert.assertEquals("Splits entry not correct", testPartition, split.getPartition());
    }
  }

  @Test
  @Category(UnitTest.class)
  public void writeSplits() throws IOException
  {
    Splits splits = new PartitionerSplit();
    splits.generateSplits(new TestGenerator());

    File splitfile = folder.newFile(testName.getMethodName());

    FileOutputStream stream = new FileOutputStream(splitfile);
    splits.writeSplits(stream);

    stream.close();

    FileInputStream in = new FileInputStream(splitfile);
    Scanner reader = new Scanner(in);

    Assert.assertEquals("Wrong number written", generated.length, reader.nextInt());

    PartitionerSplit.PartitionerSplitInfo[] si = (PartitionerSplit.PartitionerSplitInfo[]) splits.getSplits();

    for (int i = 0; i < generated.length; i++)
    {
      Assert.assertEquals("Splits entry not correct", generated[i].longValue(), reader.nextLong());
      Assert.assertEquals("Partition entry not correct", i, reader.nextLong());
    }

    reader.close();
  }

  @Test
  @Category(UnitTest.class)
  public void writeSplitsPath() throws IOException
  {
    Splits splits = new PartitionerSplit();
    splits.generateSplits(new TestGenerator());

    File splitfile = folder.newFolder(testName.getMethodName());

    splits.writeSplits(new Path(splitfile.toURI()));

    FileInputStream in = new FileInputStream(new File(splitfile, "partitions"));
    Scanner reader = new Scanner(in);

    Assert.assertEquals("Wrong number written", generated.length, reader.nextInt());

    PartitionerSplit.PartitionerSplitInfo[] si = (PartitionerSplit.PartitionerSplitInfo[]) splits.getSplits();

    for (int i = 0; i < generated.length; i++)
    {
      Assert.assertEquals("Splits entry not correct", generated[i].longValue(), reader.nextLong());
      Assert.assertEquals("Partition entry not correct", i, reader.nextLong());
    }

    reader.close();
  }

  @Test
  @Category(UnitTest.class)
  public void readSplits() throws IOException
  {
    File splitfile = folder.newFile(testName.getMethodName());

    FileOutputStream stream = new FileOutputStream(splitfile);
    PrintWriter writer = new PrintWriter(stream);

    writer.println(generated.length);
    for (int i = 0; i < generated.length; i++)
    {
      writer.print(generated[i]);
      writer.print(" ");
      writer.println(i);
    }

    writer.close();
    stream.close();

    FileInputStream in = new FileInputStream(splitfile);
    Splits splits = new PartitionerSplit();
    splits.readSplits(in);

    PartitionerSplit.PartitionerSplitInfo[] si = (PartitionerSplit.PartitionerSplitInfo[]) splits.getSplits();

    Assert.assertEquals("Splits length not correct", generated.length, si.length);
    for (int i = 0; i < generated.length; i++)
    {
      Assert.assertEquals("Splits entry not correct", generated[i].longValue(), si[i].getTileId());
    }

    in.close();
  }

  @Test
  @Category(UnitTest.class)
  public void readSplitsPath() throws IOException
  {
    File splitFolder = folder.newFolder(testName.getMethodName());
    File splitfile = new File(splitFolder, "partitions");

    FileOutputStream stream = new FileOutputStream(splitfile);
    PrintWriter writer = new PrintWriter(stream);

    writer.println(generated.length);
    for (int i = 0; i < generated.length; i++)
    {
      writer.print(generated[i]);
      writer.print(" ");
      writer.println(i);
    }

    writer.close();
    stream.close();

    Splits splits = new PartitionerSplit();
    splits.readSplits(new Path(splitFolder.toURI()));

    PartitionerSplit.PartitionerSplitInfo[] si = (PartitionerSplit.PartitionerSplitInfo[]) splits.getSplits();

    Assert.assertEquals("Splits length not correct", generated.length, si.length);
    for (int i = 0; i < generated.length; i++)
    {
      Assert.assertEquals("Splits entry not correct", generated[i].longValue(), si[i].getTileId());
    }

  }

  private long findSplit(Long[] splits, long value)
  {
    for (int i = 0; i < splits.length; i++)
    {
      if (value <= splits[i])
      {
        return i;
      }
    }
    return -1;
  }

  class TestGenerator implements SplitGenerator
  {
    @Override
    public SplitInfo[] getSplits()
    {
      PartitionerSplit.PartitionerSplitInfo splits[] = new PartitionerSplit.PartitionerSplitInfo[generated.length];
      for (int i = 0; i < splits.length; i++)
      {
        splits[i] = new PartitionerSplit.PartitionerSplitInfo(generated[i], i);
      }

      return splits;
    }

    @Override
    public SplitInfo[] getPartitions()
    {
      return getSplits();
    }
  }

  class BigTestGenerator implements SplitGenerator
  {
    Long[] generated = null;
    public BigTestGenerator()
    {
      Random rand = new Random();
      Set<Long> raw = new TreeSet<>();
      while (raw.size() < 10000)
      {
        raw.add((long) rand.nextInt(Integer.MAX_VALUE));
      }

      generated = raw.toArray(new Long[raw.size()]);
    }

    @Override
    public SplitInfo[] getSplits()
    {
      PartitionerSplit.PartitionerSplitInfo splits[] = new PartitionerSplit.PartitionerSplitInfo[generated.length];
      for (int i = 0; i < splits.length; i++)
      {
        splits[i] = new PartitionerSplit.PartitionerSplitInfo(generated[i], i);
      }

      return splits;
    }

    @Override
    public SplitInfo[] getPartitions()
    {
      return getSplits();
    }
  }

}