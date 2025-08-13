package jp.alhinc.calculate_sales;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CalculateSales {

	// 支店定義ファイル名
	private static final String FILE_NAME_BRANCH_LST = "branch.lst";

	// 商品定義ファイル名
	private static final String FILE_NAME_COMMODITY_LST = "commodity.lst";

	// 支店別集計ファイル名
	private static final String FILE_NAME_BRANCH_OUT = "branch.out";

	// 商品別集計ファイル名
	private static final String FILE_NAME_COMMODITY_OUT = "commodity.out";

	// 支店コード正規表現
	private static final String BRANCH_CODE_REGEX = "^[0-9]{3}";

	// 商品コード正規表現
	private static final String COMMODITY_CODE_REGEX = "^[a-zA-Z0-9]{8}";

	// エラーメッセージ
	private static final String UNKNOWN_ERROR = "予期せぬエラーが発生しました";
	private static final String FILE_NOT_EXIST = "ファイルが存在しません";
	private static final String FILE_INVALID_FORMAT = "のフォーマットが不正です";
	private static final String FILE_NAME_NOT_CONSECUTIVE = "売上ファイル名が連番になっていません";
	private static final String DIGIT_OVERFLOW = "合計金額が10桁を超えました";
	private static final String BRANCH_CODE_NOT_EXIST =  "の支店コードが不正です";
	private static final String COMMODITY_CODE_NOT_EXIST =  "の商品コードが不正です";

	// 日本語ファイル名（ファイル読み込み処理のエラーメッセージ分類で使用）
	private static final String JAPANESE_FILE_NAME_BRANCH_LST = "支店定義";
	private static final String JAPANESE_FILE_NAME_COMMODITY_LST = "商品定義";

	/**
	 * メインメソッド
	 *
	 * @param コマンドライン引数
	 */
	public static void main(String[] args) {

		//コマンドライン引数が渡されているか確認
		if(args.length != 1) {
			System.out.println(UNKNOWN_ERROR);
			return;
		}

		// 支店コードと支店名を保持するMap
		Map<String, String> branchNames = new HashMap<>();
		// 支店コードと売上金額を保持するMap
		Map<String, Long> branchSales = new HashMap<>();
		// 商品コードと商品名を保持するMap
		Map<String, String> commodityNames = new HashMap<>();
		// 商品コードと売上金額を保持するMap
		Map<String, Long> commoditySales = new HashMap<>();

		// 支店定義ファイル読み込み処理
		if(!readFile(args[0], FILE_NAME_BRANCH_LST, BRANCH_CODE_REGEX, JAPANESE_FILE_NAME_BRANCH_LST, branchNames, branchSales)) {
			return;
		}

		// 商品定義ファイル読み込み処理
		if(!readFile(args[0], FILE_NAME_COMMODITY_LST, COMMODITY_CODE_REGEX, JAPANESE_FILE_NAME_COMMODITY_LST, commodityNames, commoditySales)) {
			return;
		}

		//売上ファイルの情報をリストに格納
		File[] files = new File(args[0]).listFiles();
		List<File> rcdFiles = new ArrayList<>();

		//対象がファイルかつ、「数字8桁.rcd」か確認
		for(int i = 0; i < files.length; i++) {
			if(files[i].isFile() &&  files[i].getName().matches("^[0-9]{8}\\.rcd$")) {
				rcdFiles.add(files[i]);
			}
		}

		//売上ファイルが連番か確認
		Collections.sort(rcdFiles);
		for(int i = 0; i < rcdFiles.size() - 1; i++) {
			int former = Integer.parseInt(rcdFiles.get(i).getName().substring(0, 8));
			int latter = Integer.parseInt(rcdFiles.get(i + 1).getName().substring(0, 8));

			if((latter - former) != 1) {
				System.out.println(FILE_NAME_NOT_CONSECUTIVE);
				return;
			}
		}

		//売上ファイルの読み込み
		for(int i = 0; i < rcdFiles.size(); i++) {
			BufferedReader br = null;
			List<String> fileContents = new ArrayList<>();
			try {
				FileReader fr = new FileReader(rcdFiles.get(i));
				br = new BufferedReader(fr);

				String line;
				while((line = br.readLine()) != null) {
					 fileContents.add(line);
				}
			} catch(IOException e) {
				System.out.println(UNKNOWN_ERROR);
				return;
			} finally {
				// ファイルを開いている場合
				if(br != null) {
					try {
						// ファイルを閉じる
						br.close();
					} catch(IOException e) {
						System.out.println(UNKNOWN_ERROR);
						return;
					}
				}
			}

			//売上ファイルのフォーマット確認
			if(fileContents.size() != 3) {
				System.out.println(rcdFiles.get(i).getName() + FILE_INVALID_FORMAT);
				return;
			}

			//売上ファイルの支店コードが支店定義ファイルに存在するか確認
			if(!branchNames.containsKey(fileContents.get(0))) {
				System.out.println(rcdFiles.get(i).getName() + BRANCH_CODE_NOT_EXIST);
				return;
			}
			
			//売上ファイルの商品コードが商品定義ファイルに存在するか確認
			if(!commodityNames.containsKey(fileContents.get(1))) {
				System.out.println(rcdFiles.get(i).getName() + COMMODITY_CODE_NOT_EXIST);
				return;
			}

			//売上金額が数字か確認
			if(!fileContents.get(2).matches("^[0-9]*$")) {
				System.out.println(UNKNOWN_ERROR);
				return;
			}

			//売上金額の型変換
			long fileSale = Long.parseLong(fileContents.get(2));

			Long branchSaleAmount = branchSales.get(fileContents.get(0)) + fileSale;
			Long commoditySaleAmount = commoditySales.get(fileContents.get(1)) + fileSale;


			//売上金額の合計が10桁を超えたか確認
			if(branchSaleAmount >= 10000000000L) {
				System.out.println(DIGIT_OVERFLOW);
				return;
			}
			if(commoditySaleAmount >= 10000000000L) {
				System.out.println(DIGIT_OVERFLOW);
				return;
			}

			//加算した売上金額をMapに追加
			branchSales.put(fileContents.get(0), branchSaleAmount);
			commoditySales.put(fileContents.get(1), commoditySaleAmount);
		}

		// 支店別集計ファイル書き込み処理
		if(!writeFile(args[0], FILE_NAME_BRANCH_OUT, branchNames, branchSales)) {
			return;
		}
		
		// 商品別集計ファイル書き込み処理
		if(!writeFile(args[0], FILE_NAME_COMMODITY_OUT, commodityNames, commoditySales)) {
			return;
		}
	}

	/**
	 * ファイル読み込み処理（支店定義ファイル、商品定義ファイル）
	 *
	 * @param フォルダパス
	 * @param ファイル名
	 * @param コード（支店コードまたは商品コード）と名前（支店名または商品名）を保持するMap
	 * @param コード（支店コードまたは商品コード）と売上金額を保持するMap
	 * @return 読み込み可否
	 */
	private static boolean readFile(String path, String fileName, String regex, String japaneseFileName, Map<String, String> names, Map<String, Long> sales) {
		BufferedReader br = null;

		try {
			File file = new File(path, fileName);

			//ファイルが存在するか確認
			if(!file.exists()) {
				System.out.println(japaneseFileName + FILE_NOT_EXIST);
				return false;
			}

			FileReader fr = new FileReader(file);
			br = new BufferedReader(fr);
			String line;

			while((line = br.readLine()) != null) {
				String[] items = line.split(",");

				//ファイルのフォーマット確認
				if((items.length != 2) || (!items[0].matches(regex))) {
					System.out.println(japaneseFileName + FILE_INVALID_FORMAT);
					return false;
				}
				names.put(items[0], items[1]);
				sales.put(items[0], 0L);
			}
		} catch(IOException e) {
			System.out.println(UNKNOWN_ERROR);
			return false;
		} finally {
			if(br != null) {
				try {
					br.close();
				} catch(IOException e) {
					System.out.println(UNKNOWN_ERROR);
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * ファイル書き込み処理（支店別集計ファイル、商品別集計ファイル）
	 *
	 * @param フォルダパス
	 * @param ファイル名
	 * @param コード（支店コードまたは商品コード）と名前（支店名または商品名）を保持するMap
	 * @param コード（支店コードまたは商品コード）と売上金額を保持するMap
	 * @return 書き込み可否
	 */
	private static boolean writeFile(String path, String fileName, Map<String, String> names, Map<String, Long> sales) {
		BufferedWriter bw = null;

		try {
			File file = new File(path, fileName);
			FileWriter fw = new FileWriter(file);
			bw = new BufferedWriter(fw);

			for(String key : names.keySet()) {
				bw.write(key + "," + names.get(key) + "," + sales.get(key));
				bw.newLine();
			}
		} catch(IOException e) {
			System.out.println(UNKNOWN_ERROR);
			return false;
		} finally {
			if(bw != null) {
				try {
					bw.close();
				} catch(IOException e) {
					System.out.println(UNKNOWN_ERROR);
					return false;
				}
			}
		}
		return true;
	}

}
