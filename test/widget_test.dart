import 'package:aerie_mobile/app.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  group('AerieApp', () {
    testWidgets('renders home screen', (tester) async {
      await tester.pumpWidget(const AerieApp());

      expect(find.text('Aerie 接收端'), findsOneWidget);
    });
  });
}
